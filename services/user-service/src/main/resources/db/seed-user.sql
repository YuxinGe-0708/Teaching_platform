-- Idempotent user-service demo data. All demo passwords are: 123456
SET NAMES utf8mb4;
USE user_db;
SET @pwd = '$2a$10$EQsEXjoadhew/SGvon4uF.T2wNWI2XeyEqQgqxigBD4sdV6gUCHWq';

INSERT INTO `user` (id, username, password, role, name, email, status) VALUES
 (1001, 'ms_admin', @pwd, 'admin', '微服务管理员', 'ms_admin@example.com', 1),
 (1002, 'ms_teacher', @pwd, 'teacher', '微服务教师', 'ms_teacher@example.com', 1),
 (1003, 'ms_teacher2', @pwd, 'teacher', '算法教师', 'ms_teacher2@example.com', 1),
 (1004, 'ms_student', @pwd, 'student', '微服务学生', 'ms_student@example.com', 1),
 (1005, 'ms_student2', @pwd, 'student', '测试学生二', 'ms_student2@example.com', 1),
 (1006, 'ms_student3', @pwd, 'student', '测试学生三', 'ms_student3@example.com', 1)
ON DUPLICATE KEY UPDATE password=VALUES(password), role=VALUES(role), name=VALUES(name), email=VALUES(email), status=1;

INSERT INTO notification (user_id, title, content, type, is_read)
SELECT 1004, '微服务环境已就绪', '你已加入 Java 微服务实践课程，可查看资料、作业、成绩和讨论。', 'system', 0
WHERE EXISTS (SELECT 1 FROM `user` WHERE id=1004)
  AND NOT EXISTS (SELECT 1 FROM notification WHERE user_id=1004 AND title='微服务环境已就绪');
INSERT INTO notification (user_id, title, content, type, is_read)
SELECT 1002, '课程数据已准备', '课程、班级、资源和作业演示数据已经导入。', 'course', 0
WHERE EXISTS (SELECT 1 FROM `user` WHERE id=1002)
  AND NOT EXISTS (SELECT 1 FROM notification WHERE user_id=1002 AND title='课程数据已准备');
INSERT INTO notification (user_id, title, content, type, is_read)
SELECT 1001, '系统初始化完成', '三个微服务数据库已完成演示数据初始化。', 'system', 1
WHERE EXISTS (SELECT 1 FROM `user` WHERE id=1001)
  AND NOT EXISTS (SELECT 1 FROM notification WHERE user_id=1001 AND title='系统初始化完成');

INSERT INTO operation_log (user_id, username, action, detail)
SELECT 1001, 'ms_admin', '初始化演示数据', 'user-service 演示账号、通知和日志已导入'
WHERE NOT EXISTS (SELECT 1 FROM operation_log WHERE user_id=1001 AND action='初始化演示数据');
