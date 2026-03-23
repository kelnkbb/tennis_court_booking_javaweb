-- 秒杀优惠券活动与用户领取记录（执行前请备份数据库）

CREATE TABLE IF NOT EXISTS `coupon_activity` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(128) NOT NULL COMMENT '活动名称',
  `total_stock` int NOT NULL COMMENT '总库存（份）',
  `start_time` datetime NOT NULL COMMENT '抢购开始时间',
  `end_time` datetime NOT NULL COMMENT '抢购结束时间',
  `discount_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '面额（元）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0草稿 1已发布 2已结束',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_time` (`start_time`,`end_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀优惠券活动';

CREATE TABLE IF NOT EXISTS `coupon_user_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` int NOT NULL,
  `user_id` int NOT NULL,
  `coupon_code` varchar(64) NOT NULL COMMENT '券码',
  `grab_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `use_status` tinyint NOT NULL DEFAULT '0' COMMENT '0未使用 1已使用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_user` (`activity_id`,`user_id`),
  UNIQUE KEY `uk_coupon_code` (`coupon_code`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户秒杀领券记录';
