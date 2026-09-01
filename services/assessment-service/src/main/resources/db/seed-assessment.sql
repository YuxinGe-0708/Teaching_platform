-- Idempotent assessment-service demo data. Course/student IDs are logical references only.
SET NAMES utf8mb4;
USE assessment_db;

INSERT INTO task (id, title, description, course_id, type, max_score, time_limit_ms, memory_limit_mb, code_template, end_time, status)
VALUES
 (1001,'微服务架构作业','请说明 BFF 与内部服务的职责边界。',1001,'homework',100,15000,128,NULL,DATE_ADD(NOW(),INTERVAL 14 DAY),'published'),
 (1002,'两数求和编程题','读取两个整数并输出其和。\n\n<!--TP_META\ntestCases=LS0tQ0FTRS0tLQoxIDIKLS0tT1VUUFVULS0tCjMKLS0tV0VJR0hULS0tCjEKLS0tQ0FTRS0tLQoxMCAyMAotLS1PVVRQVVQtLS0KMzAKLS0tV0VJR0hULS0tCjEK\nTP_META-->','1002','programming',100,15000,128,'python:\n# write code here\n\njava:\npublic class Main { public static void main(String[] args) {} }',DATE_ADD(NOW(),INTERVAL 14 DAY),'published'),
 (1003,'数据库事务测验','判断题：事务的隔离性属于 ACID 特性。',1003,'exam',100,15000,128,NULL,DATE_ADD(NOW(),INTERVAL 14 DAY),'published'),
 (1004,'微服务考试草稿','尚未发布的考试。',1001,'exam',100,15000,128,NULL,DATE_ADD(NOW(),INTERVAL 14 DAY),'draft')
ON DUPLICATE KEY UPDATE title=VALUES(title), description=VALUES(description), status=VALUES(status), end_time=VALUES(end_time);

INSERT INTO submission (id, task_id, student_id, content, file_path, score, status, judge_result, feedback)
VALUES
 (1001,1001,1004,'已完成架构说明。','',92.0,'graded','AC','职责边界清晰。'),
 (1002,1002,1004,'a,b=map(int,input().split())\nprint(a+b)','',100.0,'graded','AC','全部测试通过。'),
 (1003,1003,1005,'是','',95.0,'graded','AC','回答正确。')
ON DUPLICATE KEY UPDATE content=VALUES(content), score=VALUES(score), status=VALUES(status), judge_result=VALUES(judge_result), feedback=VALUES(feedback);

INSERT INTO exam_record (id, task_id, student_id, start_time, submit_time, content, score, status)
VALUES
 (1001,1003,1005,DATE_SUB(NOW(),INTERVAL 30 MINUTE),NOW(),'是',95.0,'SUBMITTED'),
 (1002,1004,1004,NULL,NULL,'',NULL,'NOT_STARTED')
ON DUPLICATE KEY UPDATE content=VALUES(content), score=VALUES(score), status=VALUES(status);
