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
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "INDEX `idx_discussion_post_course` (`course_id`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `discussion_reply` ("
                + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "`post_id` BIGINT NOT NULL,"
                + "`user_id` BIGINT NOT NULL,"
                + "`content` TEXT NOT NULL,"
                + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "INDEX `idx_discussion_reply_post` (`post_id`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        ensureDefaultAdmin();
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
