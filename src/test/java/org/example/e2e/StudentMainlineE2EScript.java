package org.example.e2e;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StudentMainlineE2EScript extends E2eTestSupport {
    @Test
    void studentFullMainline() throws Exception {
        String stamp = stamp();
        String username = "s1_" + stamp.substring(Math.max(0, stamp.length() - 10));
        E2eHttpClient student = client();
        E2eHttpClient teacher = client();
        E2eDatabase db = null;
        String courseId = "";
        Path pdf = null;
        Path video = null;
        Map<String, Object> actual = new LinkedHashMap<>();
        boolean passed = false;
        String assertion = "失败：学生主线未完成。";
        try {
            db = database();
            E2eHttpClient.Response loginPage = student.get("/login");
            assertPage(loginPage, "/login");
            assertEquals("1", db.scalar("SELECT COUNT(*) FROM `user` WHERE username='teacher_demo'"));

            E2eHttpClient.Response teacherLogin = login(teacher, "teacher_demo", "123456");
            assertRedirect(teacherLogin, "/");
            E2eHttpClient.Response registration = register(student, username, "student", db);
            E2eHttpClient.Response studentLogin = login(student, username, "123456");
            assertRedirect(registration, "registered=1");
            assertRedirect(studentLogin, "/");

            Map<String, Object> course = createCourse(teacher, db, "S1", stamp, "active", "true");
            courseId = String.valueOf(course.get("id"));
            pdf = createPdfFixture();
            video = createVideoFixture();
            Map<String, Object> pdfResource = upload(teacher, db, courseId, "S1PDF" + stamp, pdf);
            Map<String, Object> videoResource = upload(teacher, db, courseId, "S1视频" + stamp, video);
            Map<String, Object> homework = createTask(teacher, db, courseId, "S1普通作业" + stamp, "homework", null);
            Map<String, Object> exam = createTask(teacher, db, courseId, "S1考试" + stamp, "exam", objectData("examAnswer", "main"));
            Map<String, Object> programming = createTask(teacher, db, courseId, "S1编程题" + stamp, "programming",
                    objectData("allowedLanguage", "python", "testCases", "---CASE---\n1 2\n---OUTPUT---\n3\n---WEIGHT---\n1\n---CASE---\n10 20\n---OUTPUT---\n30\n---WEIGHT---\n1"));

            String resourcePdfId = String.valueOf(pdfResource.get("id"));
            String resourceVideoId = String.valueOf(videoResource.get("id"));
            String homeworkId = String.valueOf(homework.get("id"));
            String examId = String.valueOf(exam.get("id"));
            String programmingId = String.valueOf(programming.get("id"));
            String studentId = userId(db, username);

            E2eHttpClient.Response browse = student.get("/student/course/selection?search=" + course.get("code"));
            E2eHttpClient.Response select = student.postForm("/student/course/select", data("courseId", courseId));
            E2eHttpClient.Response myCourses = student.get("/student/course/my");
            String enrollment = db.scalar("SELECT COUNT(*) FROM course_enrollment WHERE student_id=? AND course_id=?", studentId, courseId);
            String classCount = db.scalar("SELECT COALESCE(MAX(current_count),0) FROM course_class WHERE course_id=?", courseId);
            assertEquals("1", enrollment);
            assertEquals("1", classCount);
            assertTrue(bodyContains(browse, String.valueOf(course.get("code"))));
            assertTrue(bodyContains(myCourses, String.valueOf(course.get("name"))));

            E2eHttpClient.Response download = student.get("/student/resource/download/" + resourcePdfId);
            E2eHttpClient.Response progress = student.postJson("/student/resource/progress", objectData("resourceId", resourceVideoId, "currentTime", 30, "duration", 120));
            Map<String, Object> progressJson = jsonObject(progress.body());
            Map<String, String> progressRow = db.row(
                    "SELECT progress,last_position,duration FROM resource_progress WHERE student_id=? AND resource_id=?",
                    new String[] {"progress", "last_position", "duration"}, studentId, resourceVideoId);
            assertEquals(200, download.status);
            assertEquals("200", String.valueOf(progressJson.get("code")));
            assertEquals("25.00", progressRow.get("progress"));

            String postTitle = "学生主线讨论" + stamp;
            E2eHttpClient.Response post = student.postForm("/discussion/post", data(
                    "courseId", courseId, "title", postTitle, "content", "学生主线发帖",
                    "postType", "question", "targetRole", "teacher", "anonymous", "false"));
            String postId = id(db, "SELECT id FROM discussion_post WHERE course_id=? AND title=? ORDER BY id DESC LIMIT 1", courseId, postTitle);

            E2eHttpClient.Response homeworkSubmit = student.postMultipart("/student/task/submit",
                    data("taskId", homeworkId, "content", "学生主线普通作业提交"), "file", pdf);
            String homeworkSubmissionId = id(db, "SELECT id FROM submission WHERE task_id=? AND student_id=? ORDER BY id DESC LIMIT 1", homeworkId, studentId);
            E2eHttpClient.Response grade = teacher.postForm("/teacher/task/grade",
                    data("submissionId", homeworkSubmissionId, "score", "95", "comment", "学生主线批改反馈"));
            Map<String, String> graded = db.row("SELECT status,score,feedback FROM submission WHERE id=?",
                    new String[] {"status", "score", "feedback"}, homeworkSubmissionId);

            E2eHttpClient.Response examBegin = student.postForm("/student/exam/begin", data("taskId", examId));
            E2eHttpClient.Response examSave = student.postForm("/student/exam/save", data("taskId", examId, "content", "main"));
            E2eHttpClient.Response examSubmit = student.postForm("/student/exam/submit",
                    data("taskId", examId, "content", "main", "auto", "false", "uploadQuestionId", "1"));
            Map<String, String> examRow = db.row("SELECT status,score,content FROM exam_record WHERE task_id=? AND student_id=?",
                    new String[] {"status", "score", "content"}, examId, studentId);
            Map<String, Object> saveJson = jsonObject(examSave.body());
            assertEquals("SUBMITTED", examRow.get("status"));
            assertEquals("true", String.valueOf(saveJson.get("saved")));
            assertEquals(200, examSubmit.status);

            E2eHttpClient.Response judge = student.postJson("/api/v2/judge/submit", objectData(
                    "taskId", programmingId, "language", "python", "code", "a,b=map(int,input().split())\nprint(a+b)"));
            Map<String, Object> judgeJson = jsonObject(judge.body());
            Map<String, Object> judgeData = asMap(judgeJson.get("data"));
            E2eHttpClient.Response scores = student.get("/student/scores");
            E2eHttpClient.Response ai = student.postJson("/api/v2/ai/chat", objectData(
                    "courseId", courseId, "courseName", course.get("name"), "message", "请用一句话解释端到端测试"));
            Map<String, Object> aiJson = jsonObject(ai.body());
            Map<String, Object> aiData = asMap(aiJson.get("data"));

            actual.put("register", registration.summary());
            actual.put("login", studentLogin.summary());
            actual.put("course", course);
            actual.put("browseCourseSelection", browse.summary());
            actual.put("selectCourse", select.summary());
            actual.put("myCourses", merge("response", myCourses.summary(), "containsCourse", bodyContains(myCourses, String.valueOf(course.get("name")))));
            actual.put("dataConsistency", merge("enrollmentCount", enrollment, "classCurrentCountMax", classCount));
            actual.put("resource", merge("pdfDownload", download.summary(), "progressResponse", progressJson, "dbProgress", progressRow));
            actual.put("discussion", merge("post", post.summary(), "postId", postId));
            actual.put("homework", merge("submit", homeworkSubmit.summary(), "submissionId", homeworkSubmissionId, "grade", grade.summary(), "dbSubmission", graded));
            actual.put("exam", merge("begin", examBegin.summary(), "save", saveJson, "submit", examSubmit.summary(), "dbRecord", examRow));
            actual.put("programming", merge("http", judge.summary(), "response", judgeJson));
            actual.put("scores", merge("response", scores.summary(), "containsCourse", bodyContains(scores, String.valueOf(course.get("name")))));
            actual.put("ai", merge("http", ai.summary(), "response", aiJson));

            passed = registration.status == 302 && studentLogin.status == 302 && browse.status == 200
                    && select.status == 302 && myCourses.status == 200 && "1".equals(enrollment)
                    && "1".equals(classCount) && download.status == 200 && "200".equals(String.valueOf(progressJson.get("code")))
                    && "25.00".equals(progressRow.get("progress")) && postId != null && homeworkSubmissionId != null
                    && "graded".equals(graded.get("status")) && "95.0".equals(graded.get("score"))
                    && "SUBMITTED".equals(examRow.get("status")) && "AC".equals(String.valueOf(judgeData.get("status")))
                    && scores.status == 200 && ai.status == 200 && safe(aiData.get("reply")).length() > 0;
            assertion = passed ? "通过：学生端核心用户旅程闭环通过。" : "失败：学生端核心用户旅程存在未通过步骤，见实际输出。";
            assertTrue(passed, assertion);
        } catch (Throwable exception) {
            actual.put("exception", exception.toString());
            assertion = "失败：" + exception.getMessage();
            if (exception instanceof Error) throw (Error) exception;
            throw (Exception) exception;
        } finally {
            E2eMatrix.add(
                    "主线一_学生：注册 -> 登录 -> 选课 -> 学资源 -> 讨论 -> 作业 -> 考试 -> 编程 -> 成绩 -> AI",
                    "S000",
                    "UserController.register/login; TeacherController.createCourse/createTask/uploadResource/grade; StudentController.selectCourse/taskSubmit/examBegin/examSave/examSubmit/scoreSummary; TeachingResourceController.downloadPdf/updateProgress; DiscussionController.createPost; JudgeController.submitAndJudge; AiController.chat",
                    objectData("student", username, "coursePrefix", "S1", "resourceProgress", objectData("currentTime", 30, "duration", 120), "examAnswer", "main", "programmingLanguage", "python"),
                    "容器 frontend/backend/mysql 已启动；teacher_demo 可登录；数据库和上传测试文件可访问。",
                    "学生注册登录成功；选课、资源、讨论、普通作业批改、考试暂存提交、编程评测、成绩和 AI 闭环成功。",
                    actual,
                    assertion,
                    passed);
            cleanupCourse(db, courseId);
            cleanupUser(db, username);
            deleteTempFixture(pdf);
            deleteTempFixture(video);
            if (db != null) db.close();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<String, Object>();
    }
}
