-- Teaching Platform Database Schema
CREATE DATABASE IF NOT EXISTS teaching_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE teaching_platform;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL COMMENT 'student/teacher/admin',
    `name` VARCHAR(100) DEFAULT '',
    `email` VARCHAR(100) DEFAULT '',
    `avatar_url` VARCHAR(500) DEFAULT '',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 课程表
CREATE TABLE IF NOT EXISTS `course` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(200) NOT NULL,
    `code` VARCHAR(50) DEFAULT '',
    `description` TEXT,
    `credits` INT DEFAULT 0,
    `teacher_id` BIGINT NOT NULL,
    `invite_code` VARCHAR(20) UNIQUE,
    `cover_url` VARCHAR(500) DEFAULT '',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT 'active/archived',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`teacher_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 选课表
CREATE TABLE IF NOT EXISTS `course_enrollment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `enrolled_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_student_course` (`student_id`, `course_id`),
    FOREIGN KEY (`student_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 作业/考试表
CREATE TABLE IF NOT EXISTS `task` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT,
    `course_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL COMMENT 'homework/exam/programming',
    `max_score` INT DEFAULT 100,
    `end_time` TIMESTAMP NULL,
    `status` VARCHAR(20) DEFAULT 'published' COMMENT 'draft/published/closed',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 提交表
CREATE TABLE IF NOT EXISTS `submission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `content` TEXT,
    `file_path` VARCHAR(500) DEFAULT '',
    `score` DECIMAL(5,1) DEFAULT NULL,
    `status` VARCHAR(20) DEFAULT 'submitted' COMMENT 'submitted/grading/graded',
    `judge_result` VARCHAR(50) DEFAULT '' COMMENT 'AC/WA/CE/RE/TLE/MLE',
    `feedback` TEXT,
    `submitted_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`task_id`) REFERENCES `task`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`student_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT,
    `type` VARCHAR(20) DEFAULT 'system' COMMENT 'system/course/grade',
    `is_read` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 教学资源表
CREATE TABLE IF NOT EXISTS `resource` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `file_path` VARCHAR(500) DEFAULT '',
    `type` VARCHAR(50) DEFAULT 'other' COMMENT 'ppt/pdf/video/link/other',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 索引
CREATE INDEX idx_course_teacher ON `course`(`teacher_id`);
CREATE INDEX idx_task_course ON `task`(`course_id`);
CREATE INDEX idx_task_end_time ON `task`(`end_time`);
CREATE INDEX idx_submission_task ON `submission`(`task_id`);
CREATE INDEX idx_submission_student ON `submission`(`student_id`);
CREATE INDEX idx_notification_user ON `notification`(`user_id`, `is_read`);

-- CourseClass table (merge from dev branch)
CREATE TABLE IF NOT EXISTS `course_class` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL DEFAULT 'Default Class',
    `invite_code` VARCHAR(20) UNIQUE,
    `max_count` INT DEFAULT 100,
    `current_count` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Operation log table
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `username` VARCHAR(50) NOT NULL,
    `action` VARCHAR(200) NOT NULL,
    `detail` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
