-- 预约：用户申请取消（管理员审核）+ 支付方式记录
-- 请在连接业务库后执行一次

ALTER TABLE booking
    ADD COLUMN cancel_request_status TINYINT NOT NULL DEFAULT 0
        COMMENT '0-无 1-申请取消待管理员审核 2-管理员已驳回可再次申请' AFTER status,
    ADD COLUMN payment_channel VARCHAR(32) NULL COMMENT 'wechat/alipay/xianyu' AFTER cancel_request_status,
    ADD COLUMN pay_time DATETIME NULL COMMENT '用户确认付款时间' AFTER payment_channel;
