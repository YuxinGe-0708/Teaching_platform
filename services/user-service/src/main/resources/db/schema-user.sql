-- user-service 独占库：user_db
-- 仅 user / notification / operation_log 三张表，且只保留同库外键。
-- 注意：不再有指向其它服务库（course/task 等）的任何外键或联查。

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL COMMENT 'student/teacher/admin',
    `name` VARCHAR(100) DEFAULT '',
    `email` VARCHAR(100) DEFAULT '',
    `avatar_url` VARCHAR(500) DEFAULT '',
    `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT,
    `type` VARCHAR(20) DEFAULT 'system' COMMENT 'system/course/grade',
    `is_read` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_notification_user` (`user_id`, `is_read`),
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `username` VARCHAR(50) NOT NULL,
    `action` VARCHAR(200) NOT NULL,
    `detail` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_operation_log_user` (`user_id`),
    CONSTRAINT `fk_operation_log_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
