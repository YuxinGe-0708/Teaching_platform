-- Idempotent learning-service demo data. User IDs are references only; no cross-database FK is used.
SET NAMES utf8mb4;
USE learning_service_db;

INSERT INTO course (id, name, code, description, credits, subject_category, hours, teacher_id, invite_code, allow_join, status)
VALUES
 (1001, '微服务 Java 实践', 'MS101', 'Spring Boot、服务拆分、HTTP 调用与容器化实践。', 3, '软件工程', 48, 1002, 'MSJAVA26', 1, 'active'),
 (1002, '算法与编程实训', 'MS202', '复杂度、排序、查找与在线编程判题。', 4, '计算机', 64, 1003, 'MSALGO26', 1, 'active'),
 (1003, '数据库系统设计', 'MS301', '关系模型、SQL、索引与事务设计。', 3, '计算机', 48, 1002, 'MSDB2626', 1, 'active')
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description), teacher_id=VALUES(teacher_id), status='active', allow_join=1;

INSERT INTO course_class (id, course_id, name, invite_code, max_count, current_count)
VALUES
 (1001,1001,'Java 微服务一班','MSJAVA1',60,0),
 (1002,1002,'算法实训一班','MSALGO1',80,0),
 (1003,1003,'数据库设计一班','MSDB301',70,0)
ON DUPLICATE KEY UPDATE name=VALUES(name), max_count=VALUES(max_count);

INSERT INTO course_enrollment (student_id, course_id, class_id)
SELECT 1004,1001,1001 WHERE NOT EXISTS (SELECT 1 FROM course_enrollment WHERE student_id=1004 AND course_id=1001);
INSERT INTO course_enrollment (student_id, course_id, class_id)
SELECT 1005,1001,1001 WHERE NOT EXISTS (SELECT 1 FROM course_enrollment WHERE student_id=1005 AND course_id=1001);
INSERT INTO course_enrollment (student_id, course_id, class_id)
SELECT 1004,1002,1002 WHERE NOT EXISTS (SELECT 1 FROM course_enrollment WHERE student_id=1004 AND course_id=1002);
INSERT INTO course_enrollment (student_id, course_id, class_id)
SELECT 1006,1002,1002 WHERE NOT EXISTS (SELECT 1 FROM course_enrollment WHERE student_id=1006 AND course_id=1002);
INSERT INTO course_enrollment (student_id, course_id, class_id)
SELECT 1005,1003,1003 WHERE NOT EXISTS (SELECT 1 FROM course_enrollment WHERE student_id=1005 AND course_id=1003);
UPDATE course_class c SET current_count=(SELECT COUNT(*) FROM course_enrollment e WHERE e.class_id=c.id) WHERE c.id IN (1001,1002,1003);

INSERT INTO resource (id, course_id, title, file_path, type, chapter, file_size, download_count)
VALUES
 (1001,1001,'微服务架构讲义','uploads/demo/microservice-notes.pdf','pdf','第1章 服务拆分',102400,0),
 (1002,1001,'Docker Compose 演示视频','uploads/demo/compose-demo.mp4','video','第2章 容器化',204800,0),
 (1003,1002,'排序算法示例代码','uploads/demo/sorting-example.zip','code','第1章 排序',51200,0),
 (1004,1003,'SQL 事务练习','uploads/demo/sql-transaction.pdf','pdf','第3章 事务',76800,0)
ON DUPLICATE KEY UPDATE title=VALUES(title), file_path=VALUES(file_path), type=VALUES(type), chapter=VALUES(chapter);

INSERT INTO resource_progress (student_id, resource_id, progress, last_position, duration)
SELECT 1004,1001,65.00,390.00,600.00 WHERE NOT EXISTS (SELECT 1 FROM resource_progress WHERE student_id=1004 AND resource_id=1001);
INSERT INTO resource_progress (student_id, resource_id, progress, last_position, duration)
SELECT 1004,1002,30.00,180.00,600.00 WHERE NOT EXISTS (SELECT 1 FROM resource_progress WHERE student_id=1004 AND resource_id=1002);
INSERT INTO resource_progress (student_id, resource_id, progress, last_position, duration)
SELECT 1006,1003,100.00,0.00,0.00 WHERE NOT EXISTS (SELECT 1 FROM resource_progress WHERE student_id=1006 AND resource_id=1003);

INSERT INTO discussion_post (id, course_id, user_id, title, content, anonymous, post_type, target_role)
VALUES
 (1001,1001,1004,'如何理解 BFF？','BFF 在页面请求和三个微服务之间具体承担哪些职责？',0,'question','teacher'),
 (1002,1001,1002,'本周学习提示','请重点练习服务间 HTTP 调用和错误处理。',0,'announcement','all'),
 (1003,1002,1006,'排序算法复习心得','建议比较稳定性、时间复杂度和适用场景。',0,'share','all')
ON DUPLICATE KEY UPDATE title=VALUES(title), content=VALUES(content);
INSERT INTO discussion_reply (post_id, user_id, content, anonymous, assistant_reply)
SELECT 1001,1002,'BFF 负责页面聚合和会话适配，数据仍由所属微服务独占。',0,1
WHERE NOT EXISTS (SELECT 1 FROM discussion_reply WHERE post_id=1001 AND user_id=1002);
INSERT INTO discussion_reply (post_id, user_id, content, anonymous, assistant_reply)
SELECT 1002,1004,'收到，我会完成练习。',0,0
WHERE NOT EXISTS (SELECT 1 FROM discussion_reply WHERE post_id=1002 AND user_id=1004);

INSERT INTO study_note (id, student_id, course_id, resource_id, title, content, ai_summary, mind_map)
VALUES (1001,1004,1001,1001,'BFF 请求链路笔记','页面请求先到网关，再由 Thymeleaf BFF 通过 HTTP 调用 learning-service。','BFF 将页面模型与微服务 API 解耦。','mindmap\n  BFF 请求链路\n    网关\n    微服务 HTTP')
ON DUPLICATE KEY UPDATE title=VALUES(title), content=VALUES(content), ai_summary=VALUES(ai_summary), mind_map=VALUES(mind_map);
