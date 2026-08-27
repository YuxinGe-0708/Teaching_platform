SET NAMES utf8mb4;

-- =============================================================
-- Teaching Platform test data (idempotent; safe to re-run)
-- All test accounts use the same password: 123456
-- =============================================================
SET @pwd = '$2a$10$EQsEXjoadhew/SGvon4uF.T2wNWI2XeyEqQgqxigBD4sdV6gUCHWq';

-- users
INSERT INTO `user` (`username`, `password`, `role`, `name`, `email`)
SELECT u.username, u.password, u.role, u.name, u.email
FROM (
  SELECT 'teacher_demo' username, @pwd password, 'teacher' role, '张明老师' name, 'teacher_demo@example.com' email
  UNION ALL SELECT 'teacher_algo', @pwd, 'teacher', '李算法老师', 'teacher_algo@example.com'
  UNION ALL SELECT 'student_001', @pwd, 'student', '王一', 'student001@example.com'
  UNION ALL SELECT 'student_002', @pwd, 'student', '赵二', 'student002@example.com'
  UNION ALL SELECT 'student_003', @pwd, 'student', '孙三', 'student003@example.com'
  UNION ALL SELECT 'student_004', @pwd, 'student', '周四', 'student004@example.com'
  UNION ALL SELECT 'student_005', @pwd, 'student', '吴五', 'student005@example.com'
  UNION ALL SELECT 'student_006', @pwd, 'student', '郑六', 'student006@example.com'
  UNION ALL SELECT 'admin', @pwd, 'admin', '系统管理员', ''
) u
WHERE NOT EXISTS (SELECT 1 FROM `user` e WHERE e.username = u.username);

-- courses
INSERT INTO `course` (`name`, `code`, `description`, `credits`, `subject_category`, `hours`, `teacher_id`, `invite_code`, `allow_join`, `status`)
SELECT d.name, d.code, d.description, d.credits, d.subject, d.hours, u.id, d.invite_code, d.allow_join, d.status
FROM (
  SELECT 'Java 程序设计' name, 'CS101' code, '面向对象、集合、异常与 Spring Boot 入门。' description, 3 credits, '计算机' subject, 48 hours, 'teacher_demo' teacher, 'JAVA2026' invite_code, 1 allow_join, 'active' status
  UNION ALL SELECT '数据结构与算法', 'DS202', '线性表、树、图、排序与算法复杂度。', 4, '计算机', 64, 'teacher_algo', 'DATA2026', 1, 'active'
  UNION ALL SELECT '数据库系统', 'DB301', '关系模型、SQL、索引、事务与应用设计。', 3, '计算机', 48, 'teacher_demo', 'DB2026', 1, 'active'
  UNION ALL SELECT '人工智能导论', 'AI110', '搜索、机器学习基础与大模型辅助学习。', 2, '人工智能', 32, 'teacher_algo', 'AI2026', 0, 'draft'
  UNION ALL SELECT 'Web 前端开发', 'WEB220', 'HTML、CSS、JavaScript 与前端工程实践。', 3, '软件工程', 40, 'teacher_demo', 'WEB2026', 0, 'closed'
) d
JOIN `user` u ON u.username = d.teacher
WHERE NOT EXISTS (SELECT 1 FROM `course` c WHERE c.code = d.code);

-- classes
INSERT INTO `course_class` (`course_id`, `name`, `invite_code`, `max_count`, `current_count`)
SELECT c.id, d.name, d.invite_code, d.max_count, 0
FROM (
  SELECT 'CS101' course, 'Java-1 班' name, 'JAVA1A' invite_code, 60 max_count
  UNION ALL SELECT 'CS101', 'Java-实验班', 'JAVA2B', 40
  UNION ALL SELECT 'DS202', '算法-1 班', 'DATA1A', 80
  UNION ALL SELECT 'DB301', '数据库-1 班', 'DB1A', 70
  UNION ALL SELECT 'WEB220', '前端-归档班', 'WEB1A', 50
) d
JOIN `course` c ON c.code = d.course
WHERE NOT EXISTS (SELECT 1 FROM `course_class` cc WHERE cc.invite_code = d.invite_code);

-- enrollments
INSERT INTO `course_enrollment` (`student_id`, `course_id`, `class_id`)
SELECT st.id, c.id, cl.id
FROM (
  SELECT 'student_001' student, 'CS101' course, 'JAVA1A' class
  UNION ALL SELECT 'student_002', 'CS101', 'JAVA1A'
  UNION ALL SELECT 'student_003', 'CS101', 'JAVA2B'
  UNION ALL SELECT 'student_004', 'CS101', 'JAVA2B'
  UNION ALL SELECT 'student_001', 'DS202', 'DATA1A'
  UNION ALL SELECT 'student_002', 'DS202', 'DATA1A'
  UNION ALL SELECT 'student_005', 'DS202', 'DATA1A'
  UNION ALL SELECT 'student_006', 'DS202', 'DATA1A'
  UNION ALL SELECT 'student_003', 'DB301', 'DB1A'
  UNION ALL SELECT 'student_004', 'DB301', 'DB1A'
  UNION ALL SELECT 'student_005', 'DB301', 'DB1A'
) d
JOIN `user` st ON st.username = d.student
JOIN `course` c ON c.code = d.course
JOIN `course_class` cl ON cl.invite_code = d.class
WHERE NOT EXISTS (SELECT 1 FROM `course_enrollment` ce WHERE ce.student_id = st.id AND ce.course_id = c.id);

-- tasks
INSERT INTO `task` (`title`, `description`, `course_id`, `type`, `max_score`, `time_limit_ms`, `memory_limit_mb`, `code_template`, `end_time`, `status`)
SELECT d.title, d.description, c.id, d.type, d.max_score, 15000, 128, d.code_template, DATE_ADD(NOW(), INTERVAL 14 DAY), d.status
FROM (
  SELECT 'CS101' course, 'Java 基础语法作业' title, 'homework' type, '请完成变量、分支、循环练习，并上传实验报告。' description, 100 max_score, NULL code_template, 'published' status
  UNION ALL SELECT 'CS101', 'Java 期中客观题', 'exam', '单选题：Java 中入口方法名称是什么？\n\n<!--TP_META\nexamAnswer=bWFpbg==\nTP_META-->', 100, NULL, 'published'
  UNION ALL SELECT 'CS101', '两数求和编程题', 'programming', '读取两个整数，输出它们的和。\n\n<!--TP_META\ntestCases=LS0tQ0FTRS0tLQoxIDIKLS0tT1VUUFVULS0tCjMKLS0tV0VJR0hULS0tCjEKLS0tQ0FTRS0tLQoxMCAyMAotLS1PVVRQVVQtLS0KMzAKLS0tV0VJR0hULS0tCjIKLS0tQ0FTRS0tLQotMSA0Ci0tLU9VVFBVVC0tLQozCi0tLVdFSUdIVC0tLQox\nTP_META-->', 100, 'python:\n# 请在这里编写代码\n\njava:\npublic class Main { public static void main(String[] args) { } }\n\nc:\n#include <stdio.h>\nint main(){return 0;}', 'published'
  UNION ALL SELECT 'CS101', '未发布草稿任务', 'homework', '这是一条草稿任务，学生端不应看到。', 100, NULL, 'draft'
  UNION ALL SELECT 'DS202', '链表与栈作业', 'homework', '完成链表插入、删除和栈模拟队列的练习。', 100, NULL, 'published'
  UNION ALL SELECT 'DS202', '排序算法测验', 'exam', '填空题：快速排序平均时间复杂度是？\n\n<!--TP_META\nexamAnswer=TyhubG9nbik=\nTP_META-->', 100, NULL, 'published'
  UNION ALL SELECT 'DS202', '最大值编程题', 'programming', '输入若干整数，输出最大值。\n\n<!--TP_META\ntestCases=LS0tQ0FTRS0tLQozCjEgOCAyCi0tLU9VVFBVVC0tLQo4Ci0tLVdFSUdIVC0tLQoxCi0tLUNBU0UtLS0KNQo5IDIgNCA3IDEKLS0tT1VUUFVULS0tCjkKLS0tV0VJR0hULS0tCjE=\nTP_META-->', 100, 'python:\n# 请在这里编写代码\n\njava:\npublic class Main { public static void main(String[] args) { } }\n\nc:\n#include <stdio.h>\nint main(){return 0;}', 'published'
  UNION ALL SELECT 'DB301', 'SQL 查询作业', 'homework', '完成 SELECT、JOIN、GROUP BY 练习。', 100, NULL, 'published'
  UNION ALL SELECT 'DB301', '事务概念测验', 'exam', '判断题：事务的隔离性属于 ACID 特性。\n\n<!--TP_META\nexamAnswer=5piv\nTP_META-->', 100, NULL, 'published'
) d
JOIN `course` c ON c.code = d.course
WHERE NOT EXISTS (SELECT 1 FROM `task` t WHERE t.title = d.title AND t.course_id = c.id);

-- submissions
INSERT INTO `submission` (`task_id`, `student_id`, `content`, `file_path`, `score`, `status`, `judge_result`, `feedback`)
SELECT t.id, st.id, d.content, '', d.score, 'graded', d.judge_result, d.feedback
FROM (
  SELECT 'CS101' course, 'student_001' student, 'Java 基础语法作业' task, '已完成 Java 基础练习。' content, 92 score, 'AC' judge_result, '结构清晰，循环练习完成较好。' feedback
  UNION ALL SELECT 'CS101', 'student_002', 'Java 基础语法作业', '提交实验报告。', 81, 'AC', '分支练习有一处边界遗漏。'
  UNION ALL SELECT 'CS101', 'student_003', 'Java 基础语法作业', '补交报告。', 68, 'AC', '建议加强异常处理。'
  UNION ALL SELECT 'CS101', 'student_001', '两数求和编程题', 'a,b=map(int,input().split())\nprint(a+b)', 100, 'AC', '全部测试通过。'
  UNION ALL SELECT 'CS101', 'student_002', '两数求和编程题', 'print(input())', 30, 'WA', '输出不符合题意。'
  UNION ALL SELECT 'DS202', 'student_001', '排序算法测验', 'O(nlogn)', 100, 'AC', '回答正确。'
  UNION ALL SELECT 'DS202', 'student_005', '链表与栈作业', '完成链表代码。', 76, 'AC', '栈部分需要补充复杂度分析。'
  UNION ALL SELECT 'DB301', 'student_003', 'SQL 查询作业', 'SQL 作业提交。', 88, 'AC', 'JOIN 使用正确。'
) d
JOIN `course` c ON c.code = d.course
JOIN `task` t ON t.course_id = c.id AND t.title = d.task
JOIN `user` st ON st.username = d.student
WHERE NOT EXISTS (SELECT 1 FROM `submission` s WHERE s.task_id = t.id AND s.student_id = st.id);

-- discussion posts
INSERT INTO `discussion_post` (`course_id`, `user_id`, `title`, `content`, `anonymous`, `post_type`, `target_role`)
SELECT c.id, u.id, d.title, d.content, d.anonymous, d.post_type, d.target_role
FROM (
  SELECT 'CS101' course, 'student_001' username, 'Java 循环边界问题' title, 'for 循环里的 i < n 和 i <= n 什么时候用？' content, 1 anonymous, 'question' post_type, 'teacher' target_role
  UNION ALL SELECT 'CS101', 'teacher_demo', '第二周实验提示', '请大家重点关注输入输出格式，编程题会严格匹配输出。', 0, 'discussion', 'all'
  UNION ALL SELECT 'DS202', 'student_005', '排序算法复习心得', '建议用表格比较稳定性、时间复杂度和适用场景。', 0, 'share', 'all'
) d
JOIN `course` c ON c.code = d.course
JOIN `user` u ON u.username = d.username
WHERE NOT EXISTS (SELECT 1 FROM `discussion_post` p WHERE p.course_id = c.id AND p.title = d.title);

-- discussion reply
INSERT INTO `discussion_reply` (`post_id`, `user_id`, `content`, `anonymous`, `assistant_reply`)
SELECT p.id, u.id, '如果遍历长度为 n 的数组，通常使用 i < n；如果题目要求包含右端点，才考虑 <=。', 0, 1
FROM `discussion_post` p
JOIN `user` u ON u.username = 'teacher_demo'
WHERE p.title = 'Java 循环边界问题'
  AND NOT EXISTS (SELECT 1 FROM `discussion_reply` r WHERE r.post_id = p.id AND r.user_id = u.id AND r.content = '如果遍历长度为 n 的数组，通常使用 i < n；如果题目要求包含右端点，才考虑 <=。');

-- study note
INSERT INTO `study_note` (`student_id`, `course_id`, `title`, `content`, `mind_map`)
SELECT st.id, c.id, 'Java 循环笔记', 'for 循环适合次数明确的场景；while 循环适合条件驱动。', 'mindmap\n  Java 循环笔记\n    核心概念\n    易错点\n    复习建议'
FROM `user` st
JOIN `course` c ON c.code = 'CS101'
WHERE st.username = 'student_001'
  AND NOT EXISTS (SELECT 1 FROM `study_note` n WHERE n.student_id = st.id AND n.title = 'Java 循环笔记');

-- notifications
INSERT INTO `notification` (`user_id`, `title`, `content`, `type`, `is_read`)
SELECT u.id, d.title, d.content, 'system', 0
FROM (
  SELECT 'student_001' username, '测试数据已导入' title, '你已加入 Java 程序设计、数据结构与算法课程，可查看课程、作业、成绩和讨论区。' content
  UNION ALL SELECT 'teacher_demo', '课程测试数据已准备', '已为你的课程生成学生、班级、资源、作业和提交记录。'
) d
JOIN `user` u ON u.username = d.username
WHERE NOT EXISTS (SELECT 1 FROM `notification` n WHERE n.user_id = u.id AND n.title = d.title);

-- sync class headcounts
UPDATE `course_class` cc
SET cc.current_count = (SELECT COUNT(*) FROM `course_enrollment` ce WHERE ce.class_id = cc.id);