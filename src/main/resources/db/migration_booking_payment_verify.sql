-- 线下扫码付款：用户点「我已支付」后进入待管理员审核；审核通过后 status=已付款
-- 请在业务库执行一次

ALTER TABLE booking
    ADD COLUMN payment_verify_status TINYINT NOT NULL DEFAULT 0
        COMMENT '0-无 1-用户已确认付款待管理员审核 2-管理员已驳回可再次提交'
        AFTER pay_time;
