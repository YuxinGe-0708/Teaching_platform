SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS learning_service_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE learning_service_db;

CREATE TABLE IF NOT EXISTS `course` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(200) NOT NULL,
    `code` VARCHAR(50) DEFAULT '',
    `description` TEXT,
    `credits` INT DEFAULT 0,
    `subject_category` VARCHAR(100) DEFAULT '',
    `hours` INT DEFAULT 0,
    `teacher_id` BIGINT NOT NULL,
    `invite_code` VARCHAR(20) UNIQUE,
    `allow_join` TINYINT(1) DEFAULT 1,
    `status` VARCHAR(20) DEFAULT 'active',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE IF NOT EXISTS `course_enrollment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `class_id` BIGINT NULL,
    `enrolled_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_student_course` (`student_id`, `course_id`),
    FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `resource` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `file_path` VARCHAR(500) DEFAULT '',
    `type` VARCHAR(50) DEFAULT 'other',
    `chapter` VARCHAR(120) DEFAULT '默认章节',
    `file_size` BIGINT DEFAULT 0,
    `download_count` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `resource_progress` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `resource_id` BIGINT NOT NULL,
    `progress` DECIMAL(5,2) DEFAULT 0,
    `last_position` DECIMAL(12,2) DEFAULT 0,
    `duration` DECIMAL(12,2) DEFAULT 0,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_student_resource` (`student_id`, `resource_id`),
    FOREIGN KEY (`resource_id`) REFERENCES `resource`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `discussion_post` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `anonymous` TINYINT(1) DEFAULT 0,
    `post_type` VARCHAR(30) DEFAULT 'discussion',
    `target_role` VARCHAR(30) DEFAULT 'all',
    `target_user_id` BIGINT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `discussion_reply` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `anonymous` TINYINT(1) DEFAULT 0,
    `assistant_reply` TINYINT(1) DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`post_id`) REFERENCES `discussion_post`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `study_note` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `resource_id` BIGINT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `ai_summary` MEDIUMTEXT NULL,
    `mind_map` MEDIUMTEXT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`resource_id`) REFERENCES `resource`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_course_teacher ON `course`(`teacher_id`);
CREATE INDEX idx_course_enrollment_course ON `course_enrollment`(`course_id`);
CREATE INDEX idx_course_enrollment_student ON `course_enrollment`(`student_id`);
CREATE INDEX idx_resource_course ON `resource`(`course_id`);
CREATE INDEX idx_resource_progress_resource ON `resource_progress`(`resource_id`);
CREATE INDEX idx_discussion_post_course ON `discussion_post`(`course_id`);
CREATE INDEX idx_discussion_reply_post ON `discussion_reply`(`post_id`);
CREATE INDEX idx_study_note_student ON `study_note`(`student_id`);
CREATE INDEX idx_study_note_course ON `study_note`(`course_id`);
