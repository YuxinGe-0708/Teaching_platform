package org.example.config;

import org.example.util.TaskMetadataUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
@DependsOn("databaseInitializer")
public class TestDataSeeder {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-test-data:false}")
    private boolean seedTestData;

    public TestDataSeeder(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void seed() {
        if (!seedTestData) return;
        seedUsers();
        seedCoursesAndClasses();
        seedEnrollments();
        seedTasks();
        seedSubmissions();
        seedResources();
        seedProgress();
        seedDiscussion();
        seedNotesAndNotifications();
    }

    private void seedUsers() {
        user("teacher_demo", "teacher", "张明老师", "teacher_demo@example.com");
        user("teacher_algo", "teacher", "李算法老师", "teacher_algo@example.com");
        user("student_001", "student", "王一", "student001@example.com");
        user("student_002", "student", "赵二", "student002@example.com");
        user("student_003", "student", "孙三", "student003@example.com");
        user("student_004", "student", "周四", "student004@example.com");
        user("student_005", "student", "吴五", "student005@example.com");
        user("student_006", "student", "郑六", "student006@example.com");
    }

    private void seedCoursesAndClasses() {
        Long teacherDemo = userId("teacher_demo");
        Long teacherAlgo = userId("teacher_algo");
        course("CS101", "Java 程序设计", "计算机", 48, 3, teacherDemo, "JAVA2026", "面向对象、集合、异常与 Spring Boot 入门。", "active", true);
        course("DS202", "数据结构与算法", "计算机", 64, 4, teacherAlgo, "DATA2026", "线性表、树、图、排序与算法复杂度。", "active", true);
        course("DB301", "数据库系统", "计算机", 48, 3, teacherDemo, "DB2026", "关系模型、SQL、索引、事务与应用设计。", "active", true);
        course("AI110", "人工智能导论", "人工智能", 32, 2, teacherAlgo, "AI2026", "搜索、机器学习基础与大模型辅助学习。", "draft", false);
        course("WEB220", "Web 前端开发", "软件工程", 40, 3, teacherDemo, "WEB2026", "HTML、CSS、JavaScript 与前端工程实践。", "closed", false);

        classFor("CS101", "Java-1 班", "JAVA1A", 60);
        classFor("CS101", "Java-实验班", "JAVA2B", 40);
        classFor("DS202", "算法-1 班", "DATA1A", 80);
        classFor("DB301", "数据库-1 班", "DB1A", 70);
        classFor("WEB220", "前端-归档班", "WEB1A", 50);
    }

    private void seedEnrollments() {
        enroll("student_001", "CS101", "JAVA1A");
        enroll("student_002", "CS101", "JAVA1A");
        enroll("student_003", "CS101", "JAVA2B");
        enroll("student_004", "CS101", "JAVA2B");
        enroll("student_001", "DS202", "DATA1A");
        enroll("student_002", "DS202", "DATA1A");
        enroll("student_005", "DS202", "DATA1A");
        enroll("student_006", "DS202", "DATA1A");
        enroll("student_003", "DB301", "DB1A");
        enroll("student_004", "DB301", "DB1A");
        enroll("student_005", "DB301", "DB1A");
    }

    private void seedTasks() {
        task("CS101", "Java 基础语法作业", "homework", "请完成变量、分支、循环练习，并上传实验报告。", null, null, null, null, 100, "published");
        task("CS101", "Java 期中客观题", "exam", "单选题：Java 中入口方法名称是什么？", "main", null, null, null, 100, "published");
        task("CS101", "两数求和编程题", "programming", "读取两个整数，输出它们的和。", null, "1 2", "3", "1 2 | 3 | 1\n10 20 | 30 | 2\n-1 4 | 3 | 1", 100, "published");
        task("CS101", "未发布草稿任务", "homework", "这是一条草稿任务，学生端不应看到。", null, null, null, null, 100, "draft");

        task("DS202", "链表与栈作业", "homework", "完成链表插入、删除和栈模拟队列的练习。", null, null, null, null, 100, "published");
        task("DS202", "排序算法测验", "exam", "填空题：快速排序平均时间复杂度是？", "O(nlogn)", null, null, null, 100, "published");
        task("DS202", "最大值编程题", "programming", "输入若干整数，输出最大值。", null, "3\n1 8 2", "8", "3;1 8 2 | 8 | 1\n5;9 2 4 7 1 | 9 | 1", 100, "published");

        task("DB301", "SQL 查询作业", "homework", "完成 SELECT、JOIN、GROUP BY 练习。", null, null, null, null, 100, "published");
        task("DB301", "事务概念测验", "exam", "判断题：事务的隔离性属于 ACID 特性。", "是", null, null, null, 100, "published");
    }

    private void seedSubmissions() {
        submit("student_001", "Java 基础语法作业", "已完成 Java 基础练习。", 92, "graded", "AC", "结构清晰，循环练习完成较好。");
        submit("student_002", "Java 基础语法作业", "提交实验报告。", 81, "graded", "AC", "分支练习有一处边界遗漏。");
        submit("student_003", "Java 基础语法作业", "补交报告。", 68, "graded", "AC", "建议加强异常处理。");
        submit("student_001", "两数求和编程题", "a,b=map(int,input().split())\nprint(a+b)", 100, "graded", "AC", "全部测试通过。");
        submit("student_002", "两数求和编程题", "print(input())", 30, "graded", "WA", "输出不符合题意。");
        submit("student_001", "排序算法测验", "O(nlogn)", 100, "graded", "AC", "回答正确。");
        submit("student_005", "链表与栈作业", "完成链表代码。", 76, "graded", "AC", "栈部分需要补充复杂度分析。");
        submit("student_003", "SQL 查询作业", "SQL 作业提交。", 88, "graded", "AC", "JOIN 使用正确。");
    }

    private void seedResources() {
        resource("CS101", "第 1 章 Java 基础 PDF", "pdf", "第 1 章 Java 基础", "uploads/resources/seed/java-basic.pdf", 123456, 8);
        resource("CS101", "Java 面向对象讲解视频", "video", "第 2 章 面向对象", "uploads/resources/seed/java-oop.mp4", 3456789, 0);
        resource("CS101", "Java 代码模板包", "code", "第 3 章 实训", "uploads/resources/seed/java-template.zip", 20480, 3);
        resource("DS202", "栈与队列课件", "ppt", "第 2 章 栈和队列", "uploads/resources/seed/stack-queue.pptx", 888888, 5);
        resource("DS202", "排序算法 PDF", "pdf", "第 5 章 排序", "uploads/resources/seed/sort.pdf", 456789, 12);
        resource("DB301", "SQL 入门 PDF", "pdf", "第 1 章 SQL", "uploads/resources/seed/sql-basic.pdf", 321000, 6);
    }

    private void seedProgress() {
        progress("student_001", "Java 面向对象讲解视频", 72.5, 580, 800);
        progress("student_002", "Java 面向对象讲解视频", 35.0, 280, 800);
        progress("student_005", "排序算法 PDF", 100, 0, 0);
    }

    private void seedDiscussion() {
        post("CS101", "student_001", "question", "teacher", true, "Java 循环边界问题", "for 循环里的 i < n 和 i <= n 什么时候用？");
        post("CS101", "teacher_demo", "discussion", "all", false, "第二周实验提示", "请大家重点关注输入输出格式，编程题会严格匹配输出。");
        reply("Java 循环边界问题", "teacher_demo", "如果遍历长度为 n 的数组，通常使用 i < n；如果题目要求包含右端点，才考虑 <=。", false, true);
        post("DS202", "student_005", "share", "all", false, "排序算法复习心得", "建议用表格比较稳定性、时间复杂度和适用场景。");
    }

    private void seedNotesAndNotifications() {
        note("student_001", "CS101", "Java 循环笔记", "for 循环适合次数明确的场景；while 循环适合条件驱动。");
        notify("student_001", "测试数据已导入", "你已加入 Java 程序设计、数据结构与算法课程，可查看课程、作业、成绩和讨论区。", "system");
        notify("teacher_demo", "课程测试数据已准备", "已为你的课程生成学生、班级、资源、作业和提交记录。", "system");
    }

    private void user(String username, String role, String name, String email) {
        if (exists("SELECT COUNT(*) FROM `user` WHERE username = ?", username)) return;
        jdbcTemplate.update("INSERT INTO `user` (username, password, role, name, email) VALUES (?, ?, ?, ?, ?)",
                username, passwordEncoder.encode("123456"), role, name, email);
    }

    private void course(String code, String name, String category, int hours, int credits, Long teacherId,
                        String inviteCode, String description, String status, boolean allowJoin) {
        if (exists("SELECT COUNT(*) FROM course WHERE code = ?", code)) return;
        jdbcTemplate.update("INSERT INTO course (name, code, description, credits, subject_category, hours, teacher_id, invite_code, cover_url, allow_join, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                name, code, description, credits, category, hours, teacherId, inviteCode, "", allowJoin, status);
    }

    private void classFor(String courseCode, String name, String inviteCode, int maxCount) {
        if (exists("SELECT COUNT(*) FROM course_class WHERE invite_code = ?", inviteCode)) return;
        jdbcTemplate.update("INSERT INTO course_class (course_id, name, invite_code, max_count, current_count) VALUES (?, ?, ?, ?, 0)",
                courseId(courseCode), name, inviteCode, maxCount);
    }

    private void enroll(String username, String courseCode, String classInviteCode) {
        Long studentId = userId(username);
        Long courseId = courseId(courseCode);
        Long classId = classId(classInviteCode);
        if (studentId == null || courseId == null) return;
        if (!exists("SELECT COUNT(*) FROM course_enrollment WHERE student_id = ? AND course_id = ?", studentId, courseId)) {
            jdbcTemplate.update("INSERT INTO course_enrollment (student_id, course_id, class_id) VALUES (?, ?, ?)", studentId, courseId, classId);
        }
        updateClassCount(classId);
    }

    private void task(String courseCode, String title, String type, String markdown, String examAnswer,
                      String sampleInput, String expectedOutput, String testCases, int maxScore, String status) {
        Long courseId = courseId(courseCode);
        if (courseId == null || exists("SELECT COUNT(*) FROM task WHERE course_id = ? AND title = ?", courseId, title)) return;
        String desc = TaskMetadataUtils.buildDescription(markdown, examAnswer, sampleInput, expectedOutput, testCases);
        String template = "programming".equals(type) ? "python:\n# 请在这里编写代码\n\njava:\npublic class Main { public static void main(String[] args) { } }\n\nc:\n#include <stdio.h>\nint main(){return 0;}" : null;
        jdbcTemplate.update("INSERT INTO task (title, description, course_id, type, max_score, time_limit_ms, memory_limit_mb, code_template, end_time, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL 14 DAY), ?)",
                title, desc, courseId, type, maxScore, 15000, 128, template, status);
    }

    private void submit(String username, String taskTitle, String content, double score, String status, String judgeResult, String feedback) {
        Long studentId = userId(username);
        Long taskId = taskId(taskTitle);
        if (studentId == null || taskId == null) return;
        if (exists("SELECT COUNT(*) FROM submission WHERE student_id = ? AND task_id = ?", studentId, taskId)) return;
        jdbcTemplate.update("INSERT INTO submission (task_id, student_id, content, file_path, score, status, judge_result, feedback) VALUES (?, ?, ?, '', ?, ?, ?, ?)",
                taskId, studentId, content, score, status, judgeResult, feedback);
    }

    private void resource(String courseCode, String title, String type, String chapter, String path, long fileSize, int downloads) {
        Long courseId = courseId(courseCode);
        if (courseId == null || exists("SELECT COUNT(*) FROM resource WHERE course_id = ? AND title = ?", courseId, title)) return;
        jdbcTemplate.update("INSERT INTO resource (course_id, title, file_path, type, chapter, file_size, download_count) VALUES (?, ?, ?, ?, ?, ?, ?)",
                courseId, title, path, type, chapter, fileSize, downloads);
    }

    private void progress(String username, String resourceTitle, double percent, double position, double duration) {
        Long studentId = userId(username);
        Long resourceId = resourceId(resourceTitle);
        if (studentId == null || resourceId == null) return;
        if (exists("SELECT COUNT(*) FROM resource_progress WHERE student_id = ? AND resource_id = ?", studentId, resourceId)) return;
        jdbcTemplate.update("INSERT INTO resource_progress (student_id, resource_id, progress, last_position, duration) VALUES (?, ?, ?, ?, ?)",
                studentId, resourceId, percent, position, duration);
    }

    private void post(String courseCode, String username, String type, String targetRole, boolean anonymous, String title, String content) {
        Long courseId = courseId(courseCode);
        Long userId = userId(username);
        if (courseId == null || userId == null || exists("SELECT COUNT(*) FROM discussion_post WHERE course_id = ? AND title = ?", courseId, title)) return;
        jdbcTemplate.update("INSERT INTO discussion_post (course_id, user_id, title, content, anonymous, post_type, target_role) VALUES (?, ?, ?, ?, ?, ?, ?)",
                courseId, userId, title, content, anonymous, type, targetRole);
    }

    private void reply(String postTitle, String username, String content, boolean anonymous, boolean assistantReply) {
        Long postId = postId(postTitle);
        Long userId = userId(username);
        if (postId == null || userId == null || exists("SELECT COUNT(*) FROM discussion_reply WHERE post_id = ? AND user_id = ? AND content = ?", postId, userId, content)) return;
        jdbcTemplate.update("INSERT INTO discussion_reply (post_id, user_id, content, anonymous, assistant_reply) VALUES (?, ?, ?, ?, ?)",
                postId, userId, content, anonymous, assistantReply);
    }

    private void note(String username, String courseCode, String title, String content) {
        Long studentId = userId(username);
        Long courseId = courseId(courseCode);
        if (studentId == null || courseId == null || exists("SELECT COUNT(*) FROM study_note WHERE student_id = ? AND title = ?", studentId, title)) return;
        jdbcTemplate.update("INSERT INTO study_note (student_id, course_id, title, content, mind_map) VALUES (?, ?, ?, ?, ?)",
                studentId, courseId, title, content, "mindmap\n  " + title + "\n    核心概念\n    易错点\n    复习建议");
    }

    private void notify(String username, String title, String content, String type) {
        Long userId = userId(username);
        if (userId == null || exists("SELECT COUNT(*) FROM notification WHERE user_id = ? AND title = ?", userId, title)) return;
        jdbcTemplate.update("INSERT INTO notification (user_id, title, content, type) VALUES (?, ?, ?, ?)", userId, title, content, type);
    }

    private void updateClassCount(Long classId) {
        if (classId == null) return;
        jdbcTemplate.update("UPDATE course_class cc SET current_count = (SELECT COUNT(*) FROM course_enrollment ce WHERE ce.class_id = cc.id) WHERE cc.id = ?", classId);
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private Long userId(String username) {
        return id("SELECT id FROM `user` WHERE username = ?", username);
    }

    private Long courseId(String code) {
        return id("SELECT id FROM course WHERE code = ?", code);
    }

    private Long classId(String inviteCode) {
        return id("SELECT id FROM course_class WHERE invite_code = ?", inviteCode);
    }

    private Long taskId(String title) {
        return id("SELECT id FROM task WHERE title = ?", title);
    }

    private Long resourceId(String title) {
        return id("SELECT id FROM resource WHERE title = ?", title);
    }

    private Long postId(String title) {
        return id("SELECT id FROM discussion_post WHERE title = ?", title);
    }

    private Long id(String sql, Object... args) {
        List<Long> ids = jdbcTemplate.queryForList(sql, Long.class, args);
        return ids.isEmpty() ? null : ids.get(0);
    }
}
