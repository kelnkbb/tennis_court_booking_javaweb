-- 预约单关联优惠券抵扣（执行前请备份）

ALTER TABLE booking
    ADD COLUMN coupon_code varchar(64) NULL COMMENT '使用的券码' AFTER total_amount,
    ADD COLUMN coupon_discount decimal(10,2) NULL DEFAULT NULL COMMENT '本单抵扣金额' AFTER coupon_code,
    ADD COLUMN pay_amount decimal(10,2) NULL COMMENT '应付金额（实付）' AFTER coupon_discount;

-- 历史数据：无券时应付=原价
UPDATE booking SET pay_amount = total_amount WHERE pay_amount IS NULL;

ALTER TABLE booking MODIFY COLUMN pay_amount decimal(10,2) NOT NULL COMMENT '应付金额（实付）';

ALTER TABLE coupon_user_record
    ADD COLUMN booking_id int NULL COMMENT '使用该券的预约单' AFTER use_status,
    ADD KEY idx_booking (booking_id);
