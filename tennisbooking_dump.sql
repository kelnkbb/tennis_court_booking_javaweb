-- MySQL dump 10.13  Distrib 9.6.0, for macos15 (x86_64)
--
-- Host: 127.0.0.1    Database: tennisbooking
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `tennisbooking`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `tennisbooking` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `tennisbooking`;

--
-- Table structure for table `booking`
--

DROP TABLE IF EXISTS `booking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  `payment_verify_status` tinyint NOT NULL DEFAULT '0' COMMENT '0-无 1-用户已确认付款待管理员审核 2-管理员已驳回可再次提交',
  `coupon_code` varchar(50) DEFAULT NULL COMMENT '优惠券码',
  `coupon_discount` decimal(10,2) DEFAULT NULL COMMENT '优惠券折扣金额',
  `pay_amount` decimal(10,2) DEFAULT NULL COMMENT '实际支付金额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `booking_no` (`booking_no`),
  KEY `idx_court_date` (`court_id`,`booking_date`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_booking_date` (`booking_date`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预约表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `booking`
--

LOCK TABLES `booking` WRITE;
/*!40000 ALTER TABLE `booking` DISABLE KEYS */;
/*!40000 ALTER TABLE `booking` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `coupon_activity`
--

DROP TABLE IF EXISTS `coupon_activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_activity` (
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀优惠券活动';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coupon_activity`
--

LOCK TABLES `coupon_activity` WRITE;
/*!40000 ALTER TABLE `coupon_activity` DISABLE KEYS */;
INSERT INTO `coupon_activity` VALUES (1,'周末场地立减',100,'2026-03-23 00:00:00','2026-03-24 00:00:00',10.00,1,'2026-03-23 13:16:16','2026-03-23 13:16:25'),(2,'测试',100,'2026-03-24 00:00:00','2026-03-26 00:00:00',10.00,1,'2026-03-24 19:16:07','2026-03-24 19:16:10');
/*!40000 ALTER TABLE `coupon_activity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `coupon_user_record`
--

DROP TABLE IF EXISTS `coupon_user_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_user_record` (
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户秒杀领券记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coupon_user_record`
--

LOCK TABLES `coupon_user_record` WRITE;
/*!40000 ALTER TABLE `coupon_user_record` DISABLE KEYS */;
INSERT INTO `coupon_user_record` VALUES (1,1,8,'CP1-D768A6DB1D37','2026-03-23 13:16:56',0),(2,2,8,'CP2-144E68AF3A9C','2026-03-24 19:16:25',0);
/*!40000 ALTER TABLE `coupon_user_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `court`
--

DROP TABLE IF EXISTS `court`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `court`
--

LOCK TABLES `court` WRITE;
/*!40000 ALTER TABLE `court` DISABLE KEYS */;
INSERT INTO `court` VALUES (7,'新运行馆网球','菜鸟驿站旁',60.00,'','08:30-20:30',1,1),(8,'西山网球场','西山网球场',5.00,'','08:00-21:00',1,2),(9,'运动公园网球场','运动公园',30.00,'','08:00-21:00',1,2);
/*!40000 ALTER TABLE `court` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cs_conversation`
--

DROP TABLE IF EXISTS `cs_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cs_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` int NOT NULL COMMENT '咨询用户ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1=进行中 0=已结束',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人工客服会话';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cs_conversation`
--

LOCK TABLES `cs_conversation` WRITE;
/*!40000 ALTER TABLE `cs_conversation` DISABLE KEYS */;
INSERT INTO `cs_conversation` VALUES (1,8,1,'2026-03-25 08:13:45','2026-03-25 12:41:39');
/*!40000 ALTER TABLE `cs_conversation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cs_message`
--

DROP TABLE IF EXISTS `cs_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cs_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `sender_type` tinyint NOT NULL COMMENT '1=用户 2=客服(管理员)',
  `sender_user_id` int NOT NULL COMMENT '发送者用户ID',
  `content` text NOT NULL COMMENT '文本内容',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人工客服消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cs_message`
--

LOCK TABLES `cs_message` WRITE;
/*!40000 ALTER TABLE `cs_message` DISABLE KEYS */;
INSERT INTO `cs_message` VALUES (1,1,1,8,'你好','2026-03-25 08:17:51'),(2,1,2,7,'你好','2026-03-25 08:18:06'),(3,1,1,8,'你好','2026-03-25 12:39:08'),(4,1,1,8,'你好','2026-03-25 12:39:16'),(5,1,1,8,'你好','2026-03-25 12:41:39');
/*!40000 ALTER TABLE `cs_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_log`
--

DROP TABLE IF EXISTS `system_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `action` varchar(100) DEFAULT NULL COMMENT '操作类型',
  `detail` text COMMENT '操作详情',
  `ip` varchar(50) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_log`
--

LOCK TABLES `system_log` WRITE;
/*!40000 ALTER TABLE `system_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `system_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (7,'admin','$2a$10$lA75i1qWwxqiaQIDC80JFe9SSnkC2HGubzv4yfWCvobO4r4.HqZY2','00000000000','admin@tennis.com','系统管理员',2,1,'2026-03-18 11:02:07','2026-03-18 11:02:07'),(8,'zsz','$2a$10$G2utRCnsP3gQA7YW7QNcve1EhrrNqN7r2b3RI9xQ0f9pdMnJdO6kC','13947365211','zhoushizhaona@gmail.com','周世昭',1,1,'2026-03-18 11:08:52','2026-03-18 11:08:52'),(9,'尼尼💜','$2a$10$lMlR.eNrff9aHYSSlyBL0.vHw4FfxgTXAYmA3zpMabHNDnS2jsh3e','18504756971','cycheryl@126.com','陈越',1,1,'2026-03-18 14:46:52','2026-03-18 14:46:52');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'tennisbooking'
--

--
-- Dumping routines for database 'tennisbooking'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-26 20:58:03
