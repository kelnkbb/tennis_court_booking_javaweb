# 秒杀优惠券（Redis + Lua）

## 设计要点

- **库存**：`coupon:seckill:stock:{activityId}`，发布活动时写入，与 `coupon_activity.total_stock` 一致。
- **一人一单**：`coupon:seckill:got:{activityId}:{userId}`，Lua 内与扣减库存原子完成。
- **Lua 返回值**：`1` 成功；`0` 无库存；`-1` 已领过；`-2` Redis 未初始化。
- **落库**：Lua 成功后写 `coupon_user_record`；失败（含唯一键冲突）执行 **回滚 Lua**（`INCR` 库存 + `DEL` 用户标记）。
- **异步削峰**：`POST .../grab` 仅做活动校验后，将 `{grabId, activityId, userId}` 投递至 RabbitMQ 队列 `coupon.seckill.grab`；**消费者**中执行 Lua 扣减与 DB 落库，并将结果写入 Redis `coupon:seckill:result:{grabId}`（TTL 15 分钟）。客户端用 `GET .../seckill/result/{grabId}` 轮询 `PENDING` / `SUCCESS` / `FAILED`。Lua 返回 `-1` 时若 DB 已有券记录则幂等返回成功，减轻重复投递影响。

## API


| 说明              | 方法   | 路径                                            |
| --------------- | ---- | --------------------------------------------- |
| 活动列表（进行中）       | GET  | `/api/coupons/activities`（可匿名）                |
| 抢购（入队）          | POST | `/api/coupons/seckill/{activityId}/grab`（需登录，返回 `grabId`） |
| 抢购结果查询          | GET  | `/api/coupons/seckill/result/{grabId}`（需登录，本人）   |
| 我的未使用券（下拉用）   | GET  | `/api/coupons/my-unused`（需登录）               |
| 创建活动（草稿）        | POST | `/api/admin/coupon-activities`                |
| 发布并初始化 Redis 库存 | PUT  | `/api/admin/coupon-activities/{id}/publish`   |
| 活动列表（管理端，含草稿） | GET  | `/api/admin/coupon-activities`（管理员）         |

前端页面：管理员 **秒杀券管理**（`/admin/coupons`）创建草稿并发布；用户 **优惠券秒杀**（`/coupons`）抢购。管理员点击「发布」后，会向当前 **WebSocket 已连接** 的用户广播 `COUPON_ACTIVITY_PUBLISHED`，右上角弹出通知并可跳转抢购页。

## 数据库

执行 `src/main/resources/db/migration_coupon_seckill.sql` 建表。

预约下单抵扣需执行 `src/main/resources/db/migration_booking_coupon.sql`：`booking` 表增加 `coupon_code`、`coupon_discount`、`pay_amount`，`coupon_user_record` 增加 `booking_id`。创建预约时请求体可带 `couponCode`，后端按活动面额抵扣（不超过原价），核销成功则写回预约单并绑定券记录。

## 注意

- 生产环境应对活动结束做 **库存与 DB 对账**（可选定时任务）。
- 高并发下仍依赖 MySQL 唯一键 `uk_activity_user` 防止极端情况双写。
- 取消订单**未**自动退还优惠券（若需可后续扩展）。

