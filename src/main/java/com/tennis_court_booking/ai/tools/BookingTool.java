package com.tennis_court_booking.ai.tools;

import com.tennis_court_booking.ai.context.AiRequestContext;
import com.tennis_court_booking.pojo.entity.Booking;
import com.tennis_court_booking.pojo.entity.User;
import com.tennis_court_booking.pojo.vo.BookingVO;
import com.tennis_court_booking.pojo.vo.CourtSlotOptionsVO;
import com.tennis_court_booking.service.BookingService;
import com.tennis_court_booking.service.UserService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

@Component
public class BookingTool {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private Integer currentUserId() {
        Integer fromCtx = AiRequestContext.getUserId();
        if (fromCtx != null) {
            return fromCtx;
        }
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null || attrs.getRequest() == null) {
            return null;
        }
        Object v = attrs.getRequest().getAttribute("userId");
        if (v == null) {
            return null;
        }
        if (v instanceof Integer) {
            return (Integer) v;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return null;
    }

    /** 用户口语里可能出现 9：30 等全角冒号 */
    private static String normalizeTimeToken(String t) {
        if (t == null) {
            return null;
        }
        return t.trim().replace('：', ':');
    }

    @Tool("获取当前日期（东八区 Asia/Shanghai），格式 yyyy-MM-dd，用于把用户说的“明天/后天”换算成具体日期")
    public String todayInShanghai() {
        return LocalDate.now(SHANGHAI).toString();
    }

    @Tool("查询当前登录用户的我的预约列表")
    public List<BookingVO> myBookings() {
        Integer userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        // BookingService 的 my 接口在 Controller 内部使用 findByCondition，这里直接复用同样逻辑
        return bookingService.findByCondition(null, null, userId, null, null, null);
    }

    @Tool("判断某场地某日期某时间段是否可预约")
    public Boolean checkTimeAvailable(
            @P(value = "场地 ID", required = true) Integer courtId,
            @P(value = "日期，格式 yyyy-MM-dd", required = true) String bookingDate,
            @P(value = "开始时间，格式 HH:mm", required = true) String startTime,
            @P(value = "结束时间，格式 HH:mm", required = true) String endTime,
            @P(value = "排除的预约ID（可选），不提供则填 null，用于检查可用性时忽略某个已存在的预约", required = false) Long excludeId) {

        // 参数校验
        if (courtId == null || bookingDate == null || bookingDate.trim().isEmpty()
                || startTime == null || startTime.trim().isEmpty()
                || endTime == null || endTime.trim().isEmpty()) {
            return false;
        }

        // 如果 excludeId 为 null，使用 null 即可
        LocalDate date = LocalDate.parse(bookingDate.trim());
        LocalTime start = LocalTime.parse(normalizeTimeToken(startTime));
        LocalTime end = LocalTime.parse(normalizeTimeToken(endTime));

        return bookingService.checkTimeAvailable(courtId, date, start, end,
                excludeId == null ? null : excludeId.intValue());
    }

    @Tool("查询某场地某日期的可选时段（返回可约时段列表）")
    public CourtSlotOptionsVO getCourtSlotOptions(
            @P(value = "场地 ID", required = true) Integer courtId,
            @P(value = "日期，格式 yyyy-MM-dd", required = true) String bookingDate,
            @P(value = "排除的预约ID（可选，用于编辑回显），不提供则填 null", required = false) Long excludeBookingId) {
        if (courtId == null || bookingDate == null || bookingDate.trim().isEmpty()) {
            return null;
        }
        LocalDate date = LocalDate.parse(bookingDate.trim());
        return bookingService.getCourtSlotOptions(courtId, date,
                excludeBookingId == null ? null : excludeBookingId.intValue());
    }


    @Tool("根据预约ID查询预约详情（用于取消/支付前核对；仅返回属于当前登录用户的订单）")
    public BookingVO getBookingById(@P("预约ID") Integer bookingId) {
        if (bookingId == null) {
            return null;
        }
        Integer uid = currentUserId();
        if (uid == null) {
            return null;
        }
        BookingVO vo = bookingService.findById(bookingId);
        if (vo == null || vo.getUserId() == null || !vo.getUserId().equals(uid)) {
            return null;
        }
        return vo;
    }

    @Tool("提交用户取消申请（需用户本人、且仅在允许状态下生效）")
    public Integer requestCancel(@P("预约ID") Integer bookingId) {
        Integer userId = currentUserId();
        if (userId == null || bookingId == null) {
            return 0;
        }
        return bookingService.userRequestCancel(bookingId, userId);
    }

    @Tool("确认用户付款（需预约处于待付款状态，且支付渠道合法）")
    public Integer payBooking(
            @P("预约ID") Integer bookingId,
            @P("支付渠道 channel：wechat / alipay / xianyu") String channel) {
        Integer userId = currentUserId();
        if (userId == null || bookingId == null) {
            return 0;
        }
        String c = channel == null ? null : channel.trim();
        return bookingService.userPayBooking(bookingId, userId, c);
    }

    @Tool("创建用户预约订单（用户下单/预约时使用；若冲突或信息不全，返回失败原因）")
    public String createBooking(
            @P("场地ID") Integer courtId,
            @P("日期 bookingDate，格式 yyyy-MM-dd") String bookingDate,
            @P("开始时间 startTime，格式 HH:mm") String startTime,
            @P("结束时间 endTime，格式 HH:mm") String endTime,
            @P("联系人姓名 contactName，可空：空则使用当前登录用户的 realName，仍空则用 username") String contactName,
            @P("联系电话 contactPhone，可空：空则使用当前登录用户资料里的 phone") String contactPhone,
            @P("备注 remark，可选，不传可填空") String remark) {
        Integer userId = currentUserId();
        if (userId == null) {
            return "创建失败：未识别到当前用户，请先登录后再预约。";
        }
        if (courtId == null
                || bookingDate == null || bookingDate.trim().isEmpty()
                || startTime == null || startTime.trim().isEmpty()
                || endTime == null || endTime.trim().isEmpty()) {
            return "创建失败：缺少必要信息（场地ID、日期、开始/结束时间）。联系人若未说明将默认使用账号本人资料。";
        }
        User profile = userService.findById(userId);
        if (profile == null) {
            return "创建失败：无法加载当前用户资料。";
        }

        String name = StringUtils.hasText(contactName) ? contactName.trim() : null;
        if (!StringUtils.hasText(name)) {
            name = StringUtils.hasText(profile.getRealName()) ? profile.getRealName().trim() : null;
        }
        if (!StringUtils.hasText(name)) {
            name = profile.getUsername();
        }

        String phone = StringUtils.hasText(contactPhone) ? contactPhone.trim() : null;
        if (!StringUtils.hasText(phone)) {
            phone = profile.getPhone() == null ? null : profile.getPhone().trim();
        }
        if (!StringUtils.hasText(phone)) {
            return "创建失败：当前账号未绑定手机号，且用户未提供联系电话。请到个人资料补充手机号，或让用户口头提供联系电话后再下单。";
        }

        try {
            LocalDate date = LocalDate.parse(bookingDate.trim());
            LocalTime start = LocalTime.parse(normalizeTimeToken(startTime));
            LocalTime end = LocalTime.parse(normalizeTimeToken(endTime));

            if (!bookingService.checkTimeAvailable(courtId, date, start, end, null)) {
                return "创建失败：该时段不可预约（可能与他人冲突或超出当日可约规则），请先调用 checkTimeAvailable 或 getCourtSlotOptions 让用户换时段。";
            }

            Booking booking = new Booking();
            booking.setCourtId(courtId);
            booking.setUserId(userId);
            booking.setBookingDate(date);
            booking.setStartTime(start);
            booking.setEndTime(end);
            booking.setContactName(name);
            booking.setContactPhone(phone);
            booking.setRemark(remark == null ? null : remark.trim());
            // status/paymentChannel/payTime 等由 service/后续流程处理

            Booking created = bookingService.addBooking(booking);
            if (created == null) {
                return "创建失败：未能生成预约单。";
            }
            return "预约创建成功：预约ID=" + created.getId()
                    + "，预约单号=" + created.getBookingNo()
                    + "，联系人=" + name
                    + "，电话=" + phone
                    + "。当前状态为“待付款”，可引导用户使用支付方式（wechat / alipay / xianyu）完成付款。";
        } catch (Exception e) {
            // 避免把堆栈直接暴露给用户
            return "创建失败：" + (e.getMessage() == null ? "未知原因" : e.getMessage());
        }
    }
}

