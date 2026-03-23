package com.tennis_court_booking.coupon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennis_court_booking.cache.CouponActivityCacheManager;
import com.tennis_court_booking.coupon.async.SeckillGrabMessage;
import com.tennis_court_booking.coupon.async.SeckillGrabRedisResult;
import com.tennis_court_booking.coupon.mapper.CouponActivityMapper;
import com.tennis_court_booking.coupon.mapper.CouponUserRecordMapper;
import com.tennis_court_booking.coupon.pojo.CouponActivity;
import com.tennis_court_booking.coupon.pojo.CouponUserRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 秒杀优惠券：Redis Lua 原子扣减与一人一单；券记录落库。抢购请求由 MQ 消费者异步执行以削峰。
 */
@Slf4j
@Service
public class CouponSeckillService {

    private static final String STOCK_PREFIX = "coupon:seckill:stock:";
    private static final String USER_GOT_PREFIX = "coupon:seckill:got:";
    private static final String RESULT_PREFIX = "coupon:seckill:result:";
    private static final Duration RESULT_TTL = Duration.ofMinutes(15);

    /** Lua：扣库存 + 标记用户已抢（一人一单） */
    private static final String LUA_GRAB = """
            local stockKey = KEYS[1]
            local userKey = KEYS[2]
            local userTtl = tonumber(ARGV[1])
            local stock = redis.call('GET', stockKey)
            if stock == false then
              return -2
            end
            stock = tonumber(stock)
            if stock < 1 then
              return 0
            end
            if redis.call('EXISTS', userKey) == 1 then
              return -1
            end
            redis.call('DECR', stockKey)
            redis.call('SET', userKey, '1')
            if userTtl and userTtl > 0 then
              redis.call('EXPIRE', userKey, userTtl)
            end
            return 1
            """;

    /** Lua：DB 写入失败时回滚 Redis（库存+1，删除用户标记） */
    private static final String LUA_ROLLBACK = """
            redis.call('INCR', KEYS[1])
            redis.call('DEL', KEYS[2])
            return 1
            """;

    private final StringRedisTemplate stringRedisTemplate;
    private final CouponActivityMapper couponActivityMapper;
    private final CouponUserRecordMapper couponUserRecordMapper;
    private final ObjectMapper objectMapper;
    private final CouponActivityCacheManager couponActivityCacheManager;

    private final DefaultRedisScript<Long> grabScript;
    private final DefaultRedisScript<Long> rollbackScript;

    @Autowired
    public CouponSeckillService(StringRedisTemplate stringRedisTemplate,
                                  CouponActivityMapper couponActivityMapper,
                                  CouponUserRecordMapper couponUserRecordMapper,
                                  ObjectMapper objectMapper,
                                  CouponActivityCacheManager couponActivityCacheManager) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.couponActivityMapper = couponActivityMapper;
        this.couponUserRecordMapper = couponUserRecordMapper;
        this.objectMapper = objectMapper;
        this.couponActivityCacheManager = couponActivityCacheManager;
        DefaultRedisScript<Long> g = new DefaultRedisScript<>();
        g.setScriptText(LUA_GRAB);
        g.setResultType(Long.class);
        this.grabScript = g;
        DefaultRedisScript<Long> r = new DefaultRedisScript<>();
        r.setScriptText(LUA_ROLLBACK);
        r.setResultType(Long.class);
        this.rollbackScript = r;
    }

    public List<CouponActivity> listActiveActivities() {
        return couponActivityCacheManager.getActiveActivities(couponActivityMapper::listActivePublished);
    }

    /**
     * 发布活动时：将库存写入 Redis（与 DB total_stock 一致）。
     */
    public void initRedisStock(Integer activityId, int totalStock) {
        String key = STOCK_PREFIX + activityId;
        stringRedisTemplate.opsForValue().set(
                Objects.requireNonNull(key),
                Objects.requireNonNull(String.valueOf(totalStock)));
        log.info("秒杀券 Redis 库存已初始化 activityId={} stock={}", activityId, totalStock);
    }

    public void evictActiveActivitiesCache() {
        couponActivityCacheManager.evictAfterActivityMutation();
    }

    /**
     * 活动维度校验（API 入队前与消费者内均执行）。
     */
    public SeckillGrabResult validateActivityForGrab(CouponActivity act) {
        if (act == null) {
            return SeckillGrabResult.error("活动不存在");
        }
        if (act.getStatus() == null || act.getStatus() != 1) {
            return SeckillGrabResult.error("活动未开放");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(act.getStartTime())) {
            return SeckillGrabResult.error("活动尚未开始");
        }
        if (now.isAfter(act.getEndTime())) {
            return SeckillGrabResult.error("活动已结束");
        }
        return null;
    }

    /**
     * MQ 消费者：执行 Redis Lua + 落库，并写入轮询结果。
     */
    public void processGrabFromQueue(SeckillGrabMessage msg) {
        try {
            SeckillGrabResult r = executeGrabCore(msg.getActivityId(), msg.getUserId());
            SeckillGrabRedisResult redis = r.success()
                    ? SeckillGrabRedisResult.success(r.message(), r.couponCode(), msg.getActivityId(), msg.getUserId())
                    : SeckillGrabRedisResult.failed(r.message(), msg.getActivityId(), msg.getUserId());
            writeGrabResult(msg.getGrabId(), redis);
        } catch (Exception e) {
            log.error("秒杀异步处理失败 grabId={} activityId={} userId={}", msg.getGrabId(), msg.getActivityId(), msg.getUserId(), e);
            writeGrabResult(msg.getGrabId(), SeckillGrabRedisResult.failed("领取失败，请稍后重试", msg.getActivityId(), msg.getUserId()));
        }
    }

    /**
     * 核心：Lua 扣减 + 写库；失败回滚 Redis。Lua 返回 -1 时若 DB 已有记录则幂等返回成功（防重复投递）。
     * 单条 INSERT 由 MyBatis 执行，无需跨方法事务；避免同类自调用导致 @Transactional 不生效。
     */
    public SeckillGrabResult executeGrabCore(Integer activityId, Integer userId) {
        CouponActivity act = couponActivityMapper.findById(activityId);
        SeckillGrabResult v = validateActivityForGrab(act);
        if (v != null) {
            return v;
        }

        String stockKey = STOCK_PREFIX + activityId;
        String userKey = USER_GOT_PREFIX + activityId + ":" + userId;
        LocalDateTime now = LocalDateTime.now();
        long userTtlSec = Math.max(60, Duration.between(now, act.getEndTime()).getSeconds() + 86400 * 7);

        Long luaResult = stringRedisTemplate.execute(
                Objects.requireNonNull(grabScript),
                Objects.requireNonNull(List.of(stockKey, userKey)),
                String.valueOf(userTtlSec));

        long code = luaResult == null ? -99 : luaResult;
        if (code == -2) {
            return SeckillGrabResult.error("库存未初始化，请稍后再试");
        }
        if (code == 0) {
            return SeckillGrabResult.error("手慢了，库存已抢完");
        }
        if (code == -1) {
            CouponUserRecord existing = couponUserRecordMapper.findByActivityIdAndUserId(activityId, userId);
            if (existing != null) {
                return SeckillGrabResult.ok(existing.getCouponCode());
            }
            return SeckillGrabResult.error("每人限领一张，您已参与过本活动");
        }
        if (code != 1) {
            rollbackRedis(stockKey, userKey);
            return SeckillGrabResult.error("系统繁忙，请重试");
        }

        String couponCode = "CP" + activityId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        CouponUserRecord record = new CouponUserRecord();
        record.setActivityId(activityId);
        record.setUserId(userId);
        record.setCouponCode(couponCode);

        try {
            couponUserRecordMapper.insert(record);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("领券重复写入 DB，回滚 Redis activityId={} userId={}", activityId, userId);
            rollbackRedis(stockKey, userKey);
            CouponUserRecord existing = couponUserRecordMapper.findByActivityIdAndUserId(activityId, userId);
            if (existing != null) {
                return SeckillGrabResult.ok(existing.getCouponCode());
            }
            return SeckillGrabResult.error("每人限领一张，您已参与过本活动");
        } catch (Exception e) {
            log.error("领券写库失败，回滚 Redis", e);
            rollbackRedis(stockKey, userKey);
            return SeckillGrabResult.error("领取失败，请稍后重试");
        }

        return SeckillGrabResult.ok(couponCode);
    }

    public void writeGrabResult(String grabId, SeckillGrabRedisResult result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(
                    Objects.requireNonNull(RESULT_PREFIX + grabId),
                    Objects.requireNonNull(json),
                    Objects.requireNonNull(RESULT_TTL));
        } catch (JsonProcessingException e) {
            log.error("写入秒杀结果失败 grabId={}", grabId, e);
        }
    }

    public void deleteGrabResult(String grabId) {
        stringRedisTemplate.delete(RESULT_PREFIX + grabId);
    }

    public SeckillGrabRedisResult readGrabResult(String grabId) {
        String json = stringRedisTemplate.opsForValue().get(RESULT_PREFIX + grabId);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SeckillGrabRedisResult.class);
        } catch (JsonProcessingException e) {
            log.warn("解析秒杀结果失败 grabId={}", grabId);
            return null;
        }
    }

    private void rollbackRedis(String stockKey, String userKey) {
        try {
            stringRedisTemplate.execute(
                    Objects.requireNonNull(rollbackScript),
                    Objects.requireNonNull(List.of(stockKey, userKey)));
        } catch (Exception e) {
            log.error("Redis 回滚失败 stockKey={} userKey={}", stockKey, userKey, e);
        }
    }

    public record SeckillGrabResult(boolean success, String message, String couponCode) {
        public static SeckillGrabResult ok(String couponCode) {
            return new SeckillGrabResult(true, "领取成功", couponCode);
        }

        public static SeckillGrabResult error(String msg) {
            return new SeckillGrabResult(false, msg, null);
        }
    }
}
