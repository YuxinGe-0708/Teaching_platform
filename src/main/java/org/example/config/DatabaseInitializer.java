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

    @Value("${app.default-admin.password:admin123456}")
    private String adminPassword;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
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
                + "INDEX `idx_resource_progress_resource` (`resource_id`)"
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
                + "INDEX `idx_discussion_post_course` (`course_id`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `discussion_reply` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`post_id` BIGINT NOT NULL,"
                + "`user_id` BIGINT NOT NULL,"
                + "`content` TEXT NOT NULL,"
                + "`anonymous` TINYINT(1) DEFAULT 0,"
                + "`assistant_reply` TINYINT(1) DEFAULT 0,"
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "INDEX `idx_discussion_reply_post` (`post_id`)"
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
                + "INDEX `idx_study_note_course` (`course_id`)"
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
}
