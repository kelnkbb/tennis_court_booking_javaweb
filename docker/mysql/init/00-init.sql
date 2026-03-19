CREATE DATABASE IF NOT EXISTS tennisbooking DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE tennisbooking;

-- 关键：确保初始化 SQL 中的字符串字面量按 UTF-8 方式解析
-- 否则在某些环境下会把中文按错误字符集导入，导致后续接口返回乱码。
SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_results = utf8mb4;
SET collation_connection = utf8mb4_0900_ai_ci;

-- 从 database_dump.sql 清理后导入（移除 GTID/LOG_BIN 相关语句，避免在容器初始化时因权限/GTID_MODE 报错）

DROP TABLE IF EXISTS `booking`;
CREATE TABLE `booking` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `booking_no` varchar(32) NOT NULL COMMENT '预约单号',
  `court_id` int NOT NULL COMMENT '场地ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `booking_date` date NOT NULL COMMENT '预约日期',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `duration` decimal(5,2) NOT NULL COMMENT '预约时长（小时）',
  `unit_price` decimal(10,2) NOT NULL COMMENT '单价（元/小时）',
  `total_amount` decimal(10,2) NOT NULL COMMENT '总金额',
  `contact_name` varchar(50) NOT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) NOT NULL COMMENT '联系电话',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-已取消 1-待付款 2-已付款 3-已完成 4-已过期',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `cancel_request_status` tinyint DEFAULT '0' COMMENT '取消申请状态：0-无申请 1-申请中 2-已驳回',
  `payment_channel` varchar(50) DEFAULT NULL COMMENT '支付渠道：wechat/alipay',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `booking_no` (`booking_no`),
  KEY `idx_court_date` (`court_id`,`booking_date`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_booking_date` (`booking_date`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预约表';

INSERT INTO `booking` VALUES
  (7,'BK20260318777911',7,8,'2026-03-18','18:30:00','20:30:00',2.00,60.00,120.00,'周世昭','13947365211','',0,'2026-03-18 13:16:30','2026-03-18 14:48:02',0,'wechat','2026-03-18 14:12:53'),
  (8,'BK20260318214294',7,9,'2026-03-19','08:30:00','10:30:00',2.00,60.00,120.00,'陈越','18504756971','',0,'2026-03-18 14:47:23','2026-03-18 14:48:07',0,NULL,NULL);

DROP TABLE IF EXISTS `court`;
CREATE TABLE `court` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `address` varchar(200) DEFAULT NULL,
  `price` decimal(10,2) DEFAULT NULL COMMENT '每小时价格',
  `images` varchar(500) DEFAULT NULL COMMENT '图片URL，多个用逗号分隔',
  `open_time` varchar(20) DEFAULT NULL COMMENT '营业时间，如 08:00-22:00',
  `status` tinyint DEFAULT '1' COMMENT '1:营业 0:停业',
  `type` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `court` VALUES
  (7,'新运行馆网球','菜鸟驿站旁',60.00,'','08:30-20:30',1,1),
  (8,'西山网球场','西山网球场',5.00,'','08:00-21:00',1,2),
  (9,'运动公园网球场','运动公园',30.00,'','08:00-21:00',1,2);

DROP TABLE IF EXISTS `system_log`;
CREATE TABLE `system_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `action` varchar(100) DEFAULT NULL COMMENT '操作类型',
  `detail` text COMMENT '操作详情',
  `ip` varchar(50) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `real_name` varchar(50) DEFAULT NULL,
  `role` tinyint DEFAULT '1' COMMENT '1:普通用户 2:管理员',
  `status` tinyint DEFAULT '1' COMMENT '1:正常 0:禁用',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `user` VALUES
  (7,'admin','$2a$10$lA75i1qWwxqiaQIDC80JFe9SSnkC2HGubzv4yfWCvobO4r4.HqZY2','00000000000','admin@tennis.com','系统管理员',2,1,'2026-03-18 11:02:07','2026-03-18 11:02:07'),
  (8,'zsz','$2a$10$G2utRCnsP3gQA7YW7QNcve1EhrrNqN7r2b3RI9xQ0f9pdMnJdO6kC','13947365211','zhoushizhaona@gmail.com','周世昭',1,1,'2026-03-18 11:08:52','2026-03-18 11:08:52'),
  (9,'尼尼💜','$2a$10$lMlR.eNrff9aHYSSlyBL0.vHw4FfxgTXAYmA3zpMabHNDnS2jsh3e','18504756971','cycheryl@126.com','陈越',1,1,'2026-03-18 14:46:52','2026-03-18 14:46:52');

