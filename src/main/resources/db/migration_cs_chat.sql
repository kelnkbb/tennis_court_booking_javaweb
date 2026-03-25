-- 人工客服：会话与消息表（与 docker/mysql/init/01-cs-chat.sql 一致，便于已有库手动执行）
USE tennisbooking;

CREATE TABLE IF NOT EXISTS `cs_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` int NOT NULL COMMENT '咨询用户ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1=进行中 0=已结束',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人工客服会话';

CREATE TABLE IF NOT EXISTS `cs_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `sender_type` tinyint NOT NULL COMMENT '1=用户 2=客服(管理员)',
  `sender_user_id` int NOT NULL COMMENT '发送者用户ID',
  `content` text NOT NULL COMMENT '文本内容',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人工客服消息';
