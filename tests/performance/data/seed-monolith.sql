SET NAMES utf8mb4;
USE teaching_platform;

SET @pwd = '$2a$10$EQsEXjoadhew/SGvon4uF.T2wNWI2XeyEqQgqxigBD4sdV6gUCHWq';

INSERT INTO `user` (id, username, password, role, name, email)
VALUES
 (900001, 'perf_student', @pwd, 'student', 'Performance Student', 'perf_student@example.com'),
 (900002, 'perf_teacher', @pwd, 'teacher', 'Performance Teacher', 'perf_teacher@example.com')
ON DUPLICATE KEY UPDATE
 password=VALUES(password), role=VALUES(role), name=VALUES(name), email=VALUES(email);

INSERT INTO course
 (id, name, code, description, credits, subject_category, hours, teacher_id, invite_code, allow_join, status)
VALUES
 (900001, 'Performance Course', 'PERF101', 'Shared performance benchmark course.', 3, 'Software Engineering', 48, 900002, 'PERF9001', 1, 'active')
ON DUPLICATE KEY UPDATE
 name=VALUES(name), description=VALUES(description), teacher_id=VALUES(teacher_id), status='active', allow_join=1;

INSERT INTO course_class (id, course_id, name, invite_code, max_count, current_count)
VALUES (900001, 900001, 'Performance Class', 'PERFCLASS1', 100, 1)
ON DUPLICATE KEY UPDATE name=VALUES(name), max_count=VALUES(max_count), current_count=1;

INSERT INTO course_enrollment (student_id, course_id, class_id)
VALUES (900001, 900001, 900001)
ON DUPLICATE KEY UPDATE class_id=VALUES(class_id);

INSERT INTO task
 (id, title, description, course_id, type, max_score, time_limit_ms, memory_limit_mb, code_template, end_time, status)
VALUES
 (900001, 'Performance Judge Task', 'Shared performance benchmark task.

<!--TP_META
expectedOutput=SGVsbG8gV29ybGQ=
allowedLanguage=cHl0aG9u
TP_META-->', 900001, 'programming', 100, 15000, 128, 'python:', NULL, 'published')
ON DUPLICATE KEY UPDATE
 title=VALUES(title), description=VALUES(description), course_id=VALUES(course_id), type='programming', status='published';
