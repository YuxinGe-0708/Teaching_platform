package org.example.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.PostConstruct;

@Component
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.username:admin}")
    private String adminUsername;

    @Value("${app.default-admin.password:123456}")
    private String adminPassword;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        ensureCoreTables();

        addColumnIfMissing("course_enrollment", "class_id", "BIGINT NULL");
        addColumnIfMissing("course", "subject_category", "VARCHAR(100) DEFAULT ''");
        addColumnIfMissing("course", "hours", "INT DEFAULT 0");
        addColumnIfMissing("course", "allow_join", "TINYINT(1) DEFAULT 1");
        addColumnIfMissing("task", "time_limit_ms", "INT DEFAULT 15000");
        addColumnIfMissing("task", "memory_limit_mb", "INT DEFAULT 128");
        addColumnIfMissing("task", "code_template", "MEDIUMTEXT NULL");
        addColumnIfMissing("resource", "chapter", "VARCHAR(120) DEFAULT '默认章节'");
        addColumnIfMissing("resource", "file_size", "BIGINT DEFAULT 0");
        addColumnIfMissing("resource", "download_count", "INT DEFAULT 0");
        addColumnIfMissing("discussion_post", "anonymous", "TINYINT(1) DEFAULT 0");
        addColumnIfMissing("discussion_post", "post_type", "VARCHAR(30) DEFAULT 'discussion'");
        addColumnIfMissing("discussion_post", "target_role", "VARCHAR(30) DEFAULT 'all'");
        addColumnIfMissing("discussion_post", "target_user_id", "BIGINT NULL");
        addColumnIfMissing("discussion_reply", "anonymous", "TINYINT(1) DEFAULT 0");
        addColumnIfMissing("discussion_reply", "assistant_reply", "TINYINT(1) DEFAULT 0");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `resource_progress` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`student_id` BIGINT NOT NULL,"
                + "`resource_id` BIGINT NOT NULL,"
                + "`progress` DECIMAL(5,2) DEFAULT 0,"
                + "`last_position` DECIMAL(12,2) DEFAULT 0,"
                + "`duration` DECIMAL(12,2) DEFAULT 0,"
                + "`updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "UNIQUE KEY `uk_student_resource` (`student_id`, `resource_id`),"
                + "INDEX `idx_resource_progress_resource` (`resource_id`),"
                + "FOREIGN KEY (`student_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,"
                + "FOREIGN KEY (`resource_id`) REFERENCES `resource`(`id`) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `discussion_post` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`course_id` BIGINT NOT NULL,"
                + "`user_id` BIGINT NOT NULL,"
                + "`title` VARCHAR(200) NOT NULL,"
                + "`content` TEXT NOT NULL,"
                + "`anonymous` TINYINT(1) DEFAULT 0,"
                + "`post_type` VARCHAR(30) DEFAULT 'discussion',"
                + "`target_role` VARCHAR(30) DEFAULT 'all',"
                + "`target_user_id` BIGINT NULL,"
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "INDEX `idx_discussion_post_course` (`course_id`),"
                + "FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE,"
                + "FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `discussion_reply` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`post_id` BIGINT NOT NULL,"
                + "`user_id` BIGINT NOT NULL,"
                + "`content` TEXT NOT NULL,"
                + "`anonymous` TINYINT(1) DEFAULT 0,"
                + "`assistant_reply` TINYINT(1) DEFAULT 0,"
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "INDEX `idx_discussion_reply_post` (`post_id`),"
                + "FOREIGN KEY (`post_id`) REFERENCES `discussion_post`(`id`) ON DELETE CASCADE,"
                + "FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `study_note` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`student_id` BIGINT NOT NULL,"
                + "`course_id` BIGINT NOT NULL,"
                + "`resource_id` BIGINT NULL,"
                + "`title` VARCHAR(200) NOT NULL,"
                + "`content` TEXT NOT NULL,"
                + "`ai_summary` MEDIUMTEXT NULL,"
                + "`mind_map` MEDIUMTEXT NULL,"
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "`updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "INDEX `idx_study_note_student` (`student_id`),"
                + "INDEX `idx_study_note_course` (`course_id`),"
                + "FOREIGN KEY (`student_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,"
                + "FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE,"
                + "FOREIGN KEY (`resource_id`) REFERENCES `resource`(`id`) ON DELETE SET NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        ensureDefaultAdmin();
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName
        );
        if (tableCount == null || tableCount == 0) return;
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count > 0) return;
        jdbcTemplate.execute("ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + definition);
    }

    private void addIndexIfMissing(String tableName, String indexName, String columns) {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName
        );
        if (tableCount == null || tableCount == 0) return;
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Integer.class,
                tableName,
                indexName
        );
        if (count == null || count > 0) return;
        jdbcTemplate.execute("CREATE INDEX `" + indexName + "` ON `" + tableName + "` " + columns);
    }

    private void ensureDefaultAdmin() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE username = ?",
                Integer.class,
                adminUsername
        );
        if (count != null && count > 0) return;
        jdbcTemplate.update(
                "INSERT INTO `user` (username, password, role, name, email) VALUES (?, ?, 'admin', '系统管理员', '')",
                adminUsername,
                passwordEncoder.encode(adminPassword)
        );
    }

    private void ensureCoreTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `user` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`username` VARCHAR(50) NOT NULL UNIQUE,"
                + "`password` VARCHAR(255) NOT NULL,"
                + "`role` VARCHAR(20) NOT NULL COMMENT 'student/teacher/admin',"
                + "`name` VARCHAR(100) DEFAULT '',"
                + "`email` VARCHAR(100) DEFAULT '',"
                + "`avatar_url` VARCHAR(500) DEFAULT '',"
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "`updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `course` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`name` VARCHAR(200) NOT NULL,"
                + "`code` VARCHAR(50) DEFAULT '',"
                + "`description` TEXT,"
                + "`credits` INT DEFAULT 0,"
                + "`subject_category` VARCHAR(100) DEFAULT '',"
                + "`hours` INT DEFAULT 0,"
                + "`teacher_id` BIGINT NOT NULL,"
                + "`invite_code` VARCHAR(20) UNIQUE,"
                + "`cover_url` VARCHAR(500) DEFAULT '',"
                + "`allow_join` TINYINT(1) DEFAULT 1,"
                + "`status` VARCHAR(20) DEFAULT 'active',"
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "`updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "FOREIGN KEY (`teacher_id`) REFERENCES `user`(`id`) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `course_enrollment` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`student_id` BIGINT NOT NULL,"
                + "`course_id` BIGINT NOT NULL,"
                + "`class_id` BIGINT NULL,"
                + "`enrolled_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "UNIQUE KEY `uk_student_course` (`student_id`, `course_id`),"
                + "FOREIGN KEY (`student_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,"
                + "FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `task` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`title` VARCHAR(200) NOT NULL,"
                + "`description` TEXT,"
                + "`course_id` BIGINT NOT NULL,"
                + "`type` VARCHAR(20) NOT NULL COMMENT 'homework/exam/programming',"
                + "`max_score` INT DEFAULT 100,"
                + "`time_limit_ms` INT DEFAULT 15000,"
                + "`memory_limit_mb` INT DEFAULT 128,"
                + "`code_template` MEDIUMTEXT NULL,"
                + "`end_time` TIMESTAMP NULL,"
                + "`status` VARCHAR(20) DEFAULT 'published',"
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "`updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `submission` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`task_id` BIGINT NOT NULL,"
                + "`student_id` BIGINT NOT NULL,"
                + "`content` TEXT,"
                + "`file_path` VARCHAR(500) DEFAULT '',"
                + "`score` DECIMAL(5,1) DEFAULT NULL,"
                + "`status` VARCHAR(20) DEFAULT 'submitted',"
                + "`judge_result` VARCHAR(50) DEFAULT '',"
                + "`feedback` TEXT,"
                + "`submitted_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY (`task_id`) REFERENCES `task`(`id`) ON DELETE CASCADE,"
                + "FOREIGN KEY (`student_id`) REFERENCES `user`(`id`) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `notification` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`user_id` BIGINT NOT NULL,"
                + "`title` VARCHAR(200) NOT NULL,"
                + "`content` TEXT,"
                + "`type` VARCHAR(20) DEFAULT 'system',"
                + "`is_read` BOOLEAN DEFAULT FALSE,"
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `resource` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`course_id` BIGINT NOT NULL,"
                + "`title` VARCHAR(200) NOT NULL,"
                + "`file_path` VARCHAR(500) DEFAULT '',"
                + "`type` VARCHAR(50) DEFAULT 'other',"
                + "`chapter` VARCHAR(120) DEFAULT '默认章节',"
                + "`file_size` BIGINT DEFAULT 0,"
                + "`download_count` INT DEFAULT 0,"
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `course_class` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`course_id` BIGINT NOT NULL,"
                + "`name` VARCHAR(100) NOT NULL DEFAULT 'Default Class',"
                + "`invite_code` VARCHAR(20) UNIQUE,"
                + "`max_count` INT DEFAULT 100,"
                + "`current_count` INT DEFAULT 0,"
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY (`course_id`) REFERENCES `course`(`id`) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `operation_log` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`user_id` BIGINT NOT NULL,"
                + "`username` VARCHAR(50) NOT NULL,"
                + "`action` VARCHAR(200) NOT NULL,"
                + "`detail` TEXT,"
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        addIndexIfMissing("course", "idx_course_teacher", "(`teacher_id`)");
        addIndexIfMissing("task", "idx_task_course", "(`course_id`)");
        addIndexIfMissing("task", "idx_task_end_time", "(`end_time`)");
        addIndexIfMissing("submission", "idx_submission_task", "(`task_id`)");
        addIndexIfMissing("submission", "idx_submission_student", "(`student_id`)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `exam_record` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`task_id` BIGINT NOT NULL,"
                + "`student_id` BIGINT NOT NULL,"
                + "`start_time` TIMESTAMP NULL COMMENT '开始答题时间',"
                + "`submit_time` TIMESTAMP NULL COMMENT '交卷时间',"
                + "`content` TEXT COMMENT '答题内容',"
                + "`score` DECIMAL(5,1) DEFAULT NULL,"
                + "`status` VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NOT_STARTED/IN_PROGRESS/SUBMITTED/AUTO_SUBMITTED',"
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "`updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "UNIQUE KEY `uk_exam_student_task` (`student_id`, `task_id`),"
                + "FOREIGN KEY (`task_id`) REFERENCES `task`(`id`) ON DELETE CASCADE,"
                + "FOREIGN KEY (`student_id`) REFERENCES `user`(`id`) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        addIndexIfMissing("notification", "idx_notification_user", "(`user_id`, `is_read`)");
    }
}
