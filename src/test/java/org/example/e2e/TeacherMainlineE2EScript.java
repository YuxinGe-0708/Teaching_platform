package org.example.e2e;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TeacherMainlineE2EScript extends E2eTestSupport {
    @Test
    void teacherFullMainline() throws Exception {
        String stamp = stamp();
        String teacherUsername = "t2_" + stamp.substring(Math.max(0, stamp.length() - 10));
        boolean studentCreatedByTest = false;
        E2eHttpClient teacher = client();
        E2eHttpClient student = client();
        E2eDatabase db = null;
        String courseId = "";
        Path pdf = null;
        Path video = null;
        Map<String, Object> actual = new LinkedHashMap<>();
        boolean passed = false;
        String assertion = "失败：教师主线未完成。";
        try {
            db = database();
            E2eHttpClient.Response registration = register(teacher, teacherUsername, "teacher", db);
            E2eHttpClient.Response teacherLogin = login(teacher, teacherUsername, "123456");
            assertRedirect(registration, "registered=1");
            assertRedirect(teacherLogin, "/");

            Map<String, Object> course = createCourse(teacher, db, "T2", stamp, "active", "true");
            courseId = String.valueOf(course.get("id"));
            Map<String, Object> classInfo = createClass(teacher, db, courseId, "教师主线班级" + stamp, 20);
            pdf = createPdfFixture();
            video = createVideoFixture();
            Map<String, Object> pdfResource = upload(teacher, db, courseId, "T2PDF" + stamp, pdf);
            Map<String, Object> videoResource = upload(teacher, db, courseId, "T2视频" + stamp, video);
            Map<String, Object> homework = createTask(teacher, db, courseId, "T2普通作业" + stamp, "homework", null);
            Map<String, Object> exam = createTask(teacher, db, courseId, "T2考试" + stamp, "exam", objectData("examAnswer", "main"));
            Map<String, Object> programming = createTask(teacher, db, courseId, "T2编程题" + stamp, "programming",
                    objectData("allowedLanguage", "python", "testCases", "---CASE---\n1 2\n---OUTPUT---\n3\n---WEIGHT---\n1\n---CASE---\n10 20\n---OUTPUT---\n30\n---WEIGHT---\n1"));

            AccountFixture studentAccount = ensureAccount(db, student, "student_005", "student");
            studentCreatedByTest = studentAccount.createdByTest;
            String studentId = userId(db, studentAccount.username);
            String homeworkId = String.valueOf(homework.get("id"));
            String examId = String.valueOf(exam.get("id"));
            String programmingId = String.valueOf(programming.get("id"));
            E2eHttpClient.Response studentLogin = login(student, studentAccount.username, studentAccount.password);
            E2eHttpClient.Response select = student.postForm("/student/course/select", data("courseId", courseId));
            E2eHttpClient.Response homeworkSubmit = student.postForm("/student/task/submit", data("taskId", homeworkId, "content", "教师主线学生作业"));
            E2eHttpClient.Response judge = student.postJson("/api/v2/judge/submit", objectData(
                    "taskId", programmingId, "language", "python", "code", "a,b=map(int,input().split())\nprint(a+b)"));
            student.postForm("/student/exam/begin", data("taskId", examId));
            E2eHttpClient.Response examSubmit = student.postForm("/student/exam/submit", data(
                    "taskId", examId, "content", "main", "auto", "false", "uploadQuestionId", "1"));

            String homeworkSubmissionId = id(db, "SELECT id FROM submission WHERE task_id=? AND student_id=? ORDER BY id DESC LIMIT 1", homeworkId, studentId);
            E2eHttpClient.Response grade = teacher.postForm("/teacher/task/grade", data(
                    "submissionId", homeworkSubmissionId, "score", "92", "comment", "教师主线批改"));
            String programmingSubmissionId = id(db, "SELECT id FROM submission WHERE task_id=? AND student_id=? ORDER BY id DESC LIMIT 1", programmingId, studentId);
            E2eHttpClient.Response review = teacher.postForm("/teacher/task/grade", data(
                    "submissionId", programmingSubmissionId, "score", "90", "comment", "教师复核覆盖自动评分"));
            Map<String, String> graded = db.row("SELECT status,score,feedback FROM submission WHERE id=?",
                    new String[] {"status", "score", "feedback"}, homeworkSubmissionId);
            Map<String, String> reviewed = db.row("SELECT status,score,feedback FROM submission WHERE id=?",
                    new String[] {"status", "score", "feedback"}, programmingSubmissionId);
            E2eHttpClient.Response statistics = teacher.get("/teacher/score/statistics");
            E2eHttpClient.Response export = teacher.get("/teacher/score/export?courseId=" + courseId);
            E2eHttpClient.Response archive = teacher.get("/teacher/course/archive/" + courseId);
            String courseStatus = db.scalar("SELECT status FROM course WHERE id=?", courseId);
            Map<String, Object> judgeJson = jsonObject(judge.body());
            Map<String, Object> judgeData = judgeJson.get("data") instanceof Map
                    ? (Map<String, Object>) judgeJson.get("data") : new LinkedHashMap<String, Object>();

            actual.put("registerTeacher", registration.summary());
            actual.put("loginTeacher", teacherLogin.summary());
            actual.put("course", course);
            actual.put("class", classInfo);
            actual.put("resources", merge("pdf", pdfResource, "video", videoResource));
            actual.put("tasks", merge("homework", homework, "exam", exam, "programming", programming));
            actual.put("studentFixture", merge("username", studentAccount.username, "createdByTest", studentAccount.createdByTest,
                    "login", studentLogin.summary(), "select", select.summary(),
                    "homeworkSubmit", homeworkSubmit.summary(), "examSubmit", examSubmit.summary(), "judge", judgeJson));
            actual.put("gradeHomework", merge("response", grade.summary(), "dbSubmission", graded));
            actual.put("reviewProgrammingScore", merge("response", review.summary(), "dbSubmission", reviewed));
            actual.put("statistics", merge("response", statistics.summary(), "containsCourse", bodyContains(statistics, String.valueOf(course.get("name")))));
            actual.put("export", merge("response", export.summary(), "contentType", export.headers.get("Content-Type"), "containsStudent", bodyContains(export, studentAccount.username)));
            actual.put("archive", merge("response", archive.summary(), "dbStatus", courseStatus));

            passed = registration.status == 302 && teacherLogin.status == 302 && courseId.length() > 0
                    && String.valueOf(classInfo.get("id")).length() > 0 && String.valueOf(pdfResource.get("id")).length() > 0
                    && String.valueOf(videoResource.get("id")).length() > 0 && homeworkSubmissionId.length() > 0
                    && "graded".equals(graded.get("status")) && "92.0".equals(graded.get("score"))
                    && "graded".equals(reviewed.get("status")) && "90.0".equals(reviewed.get("score"))
                    && "AC".equals(String.valueOf(judgeData.get("status"))) && statistics.status == 200
                    && export.status == 200 && bodyContains(export, studentAccount.username) && "archived".equals(courseStatus);
            assertion = passed ? "通过：教师端内容生产与管理闭环通过。" : "失败：教师端闭环存在未通过步骤，见实际输出。";
            assertTrue(passed, assertion);
        } catch (Throwable exception) {
            actual.put("exception", exception.toString());
            assertion = "失败：" + exception.getMessage();
            if (exception instanceof Error) throw (Error) exception;
            throw (Exception) exception;
        } finally {
            E2eMatrix.add(
                    "主线二_教师：注册 -> 登录 -> 创建课程/班级 -> 发资源/任务 -> 批改复核 -> 统计 -> 归档",
                    "T000",
                    "UserController.register/login; TeacherController.createCourse/createClass/createTask/grade/scoreStatistics/exportScores/archiveCourse; TeachingResourceController.uploadResource; StudentController.selectCourse/taskSubmit/examBegin/examSubmit; JudgeController.submitAndJudge",
                    objectData("teacher", teacherUsername, "coursePrefix", "T2", "classMaxCount", 20, "tasks", new String[] {"homework", "exam", "programming"}, "reviewScore", 90),
                    "容器 frontend/backend/mysql 已启动；测试教师可注册登录；测试学生可注册或登录；数据库和上传测试文件可访问。",
                    "教师创建课程、班级、资源和任务；学生提交；教师批改和复核；统计、导出可用；课程归档成功。",
                    actual,
                    assertion,
                    passed);
            cleanupCourse(db, courseId);
            cleanupUser(db, teacherUsername);
            if (studentCreatedByTest) cleanupUser(db, "student_005");
            deleteTempFixture(pdf);
            deleteTempFixture(video);
            if (db != null) db.close();
        }
    }
}
