package org.example.e2e;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@EnabledIf("microserviceEnvironmentAvailable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MicroserviceMainlineE2EScript extends E2eTestSupport {

    private static final String PASSWORD = "123456";
    private static final int ENVIRONMENT_CHECK_TIMEOUT_MS = 800;

    static boolean microserviceEnvironmentAvailable() {
        return httpAvailable(E2eConfig.BASE_URL + "/healthz")
                && httpAvailable(E2eConfig.USER_SERVICE_BASE_URL + "/actuator/health")
                && httpAvailable(E2eConfig.LEARNING_SERVICE_BASE_URL + "/actuator/health")
                && httpAvailable(E2eConfig.ASSESSMENT_SERVICE_BASE_URL + "/actuator/health")
                && mysqlPortAvailable();
    }

    private static boolean httpAvailable(String endpoint) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setConnectTimeout(ENVIRONMENT_CHECK_TIMEOUT_MS);
            connection.setReadTimeout(ENVIRONMENT_CHECK_TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            return connection.getResponseCode() == 200;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static boolean mysqlPortAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(
                    E2eConfig.DB_HOST, Integer.parseInt(E2eConfig.DB_PORT)),
                    ENVIRONMENT_CHECK_TIMEOUT_MS);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Test
    @Order(1)
    void environmentPreparation() throws Exception {
        E2eDatabase userDb = null;
        E2eDatabase learningDb = null;
        E2eDatabase assessmentDb = null;
        Map<String, Object> actual = new LinkedHashMap<>();
        boolean passed = false;
        String assertion = "失败：三微服务环境不可用。";
        try {
            userDb = userDatabase();
            learningDb = learningDatabase();
            assessmentDb = assessmentDatabase();

            E2eHttpClient.Response userVersion = userClient().get("/api/version");
            E2eHttpClient.Response userHealth = userClient().get("/actuator/health");
            E2eHttpClient.Response learningVersion = learningClient().get("/api/version");
            E2eHttpClient.Response learningHealth = learningClient().get("/actuator/health");
            E2eHttpClient.Response assessmentHealth = assessmentClient().get("/actuator/health");

            String adminCount = userDb.scalar("SELECT COUNT(*) FROM `user` WHERE role='admin' AND status=1");
            String courseCount = learningDb.scalar("SELECT COUNT(*) FROM course");
            String taskCount = assessmentDb.scalar("SELECT COUNT(*) FROM task");
            actual.put("config", E2eConfig.summary());
            actual.put("userService", merge("version", userVersion.summary(), "health", userHealth.summary()));
            actual.put("learningService", merge("version", learningVersion.summary(), "health", learningHealth.summary()));
            actual.put("assessmentService", assessmentHealth.summary());
            actual.put("database", merge("adminCount", adminCount, "courseCount", courseCount, "taskCount", taskCount));

            passed = apiCode(userVersion) == 200 && apiCode(userHealth) == 200
                    && apiCode(learningVersion) == 200 && apiCode(learningHealth) == 200
                    && apiCode(assessmentHealth) == 200 && Integer.parseInt(adminCount) > 0
                    && Integer.parseInt(courseCount) > 0 && Integer.parseInt(taskCount) > 0;
            assertion = passed ? "通过：三微服务、三业务库及演示数据均已就绪。" : "失败：基础服务或数据库准备不完整。";
            Assertions.assertTrue(passed, assertion);
        } catch (Throwable exception) {
            actual.put("exception", exception.toString());
            assertion = "失败：" + exception.getMessage();
            rethrow(exception);
        } finally {
            E2eMatrix.add(
                    "准备工作：部署所有服务 + 准备测试数据", "E000",
                    "Java/JUnit5: GET /api/version, GET /actuator/health, JDBC SELECT",
                    E2eConfig.summary(),
                    "user-service、learning-service、assessment-service 已启动；三库已初始化种子数据。",
                    "三个服务健康检查和版本接口返回 200；三个数据库可查询且包含基础数据。",
                    actual, assertion, passed);
            close(userDb);
            close(learningDb);
            close(assessmentDb);
        }
    }

    @Test
    @Order(2)
    void studentMainline() throws Exception {
        String stamp = stamp();
        String suffix = stamp.substring(stamp.length() - 10);
        String teacherUsername = "e2e_st_" + suffix;
        String studentUsername = "e2e_ss_" + suffix;
        E2eDatabase userDb = null;
        E2eDatabase learningDb = null;
        E2eDatabase assessmentDb = null;
        Map<String, Object> actual = new LinkedHashMap<>();
        boolean passed = false;
        String assertion = "失败：学生主线未完成。";
        Long courseId = null;
        Long classId = null;
        Long resourceId = null;
        Long homeworkId = null;
        Long examId = null;
        Long programmingId = null;
        try {
            userDb = userDatabase();
            learningDb = learningDatabase();
            assessmentDb = assessmentDatabase();

            RegistrationResult teacher = register(userClient(), teacherUsername, "teacher", "E2E 教师");
            RegistrationResult student = register(userClient(), studentUsername, "student", "E2E 学生");
            AuthResult studentLogin = login(userClient(), studentUsername, PASSWORD);

            EntityResult course = createCourse(teacher.userId, "学生全流程课程 " + stamp,
                    "E2E-S-" + stamp.substring(stamp.length() - 6), true, "active");
            courseId = course.id;
            EntityResult clazz = createClass(courseId, "学生全流程班级", 30);
            classId = clazz.id;
            EntityResult resource = createResource(courseId, "E2E 视频资源", "uploads/demo/e2e.mp4",
                    "video", "第1章", 1024);
            resourceId = resource.id;

            E2eHttpClient.Response coursesBefore = learningClient().get("/api/courses?studentId=" + student.userId);
            E2eHttpClient.Response enroll = learningClient().postForm("/api/enrollments", data(
                    "studentId", student.userId, "courseId", courseId, "classId", classId));
            E2eHttpClient.Response myCourses = learningClient().get("/api/courses?studentId=" + student.userId);
            E2eHttpClient.Response enrollmentCheck = learningClient().get(
                    "/internal/enrollments/check?studentId=" + student.userId + "&courseId=" + courseId, internalHeaders());

            E2eHttpClient.Response progressSave = learningClient().postForm("/api/resource-progress", data(
                    "studentId", student.userId, "resourceId", resourceId,
                    "progress", 50, "lastPosition", 300, "duration", 600));
            E2eHttpClient.Response progressRead = learningClient().get(
                    "/api/resource-progress?studentId=" + student.userId + "&resourceId=" + resourceId);

            E2eHttpClient.Response post = learningClient().postForm("/api/discussions/posts", data(
                    "courseId", courseId, "userId", student.userId,
                    "title", "E2E 学生提问", "content", "如何理解服务边界？"));
            Long postId = longValue(asMap(apiValue(post)).get("id"));
            E2eHttpClient.Response reply = learningClient().postForm("/api/discussions/replies", data(
                    "postId", postId, "userId", teacher.userId, "content", "请结合 BFF 和领域服务理解。"));
            EntityResult note = createNote(student.userId, courseId, resourceId, "E2E 学习笔记", "服务边界和选课一致性");

            homeworkId = createTask(courseId, "E2E 普通作业", "homework",
                    "请说明 BFF 与领域服务的职责。", null, null).id;
            examId = createTask(courseId, "E2E 客观考试", "exam",
                    metadata("判断题：事务的隔离性属于 ACID 特性。", "正确", null, null), null, null).id;
            programmingId = createTask(courseId, "E2E 编程作业", "programming",
                    metadata("读取两个整数并输出其和。", null,
                            "---CASE---\n1 2\n---OUTPUT---\n3\n---CASE---\n10 20\n---OUTPUT---\n30", "python"),
                    null, null).id;

            E2eHttpClient.Response homeworkSubmission = assessmentClient().postForm("/internal/submissions", data(
                    "taskId", homeworkId, "studentId", student.userId,
                    "content", "BFF 负责会话适配和请求编排。", "filePath", "uploads/demo/e2e-answer.txt"), internalHeaders());
            Long submissionId = longValue(asMap(apiValue(homeworkSubmission)).get("id"));
            E2eHttpClient.Response graded = assessmentClient().postForm(
                    "/internal/submissions/" + submissionId + "/grade",
                    data("score", 88, "feedback", "E2E 批改通过"), internalHeaders());

            E2eHttpClient.Response examBegin = assessmentClient().postForm(
                    "/internal/exams/" + examId + "/begin", data("studentId", student.userId), internalHeaders());
            E2eHttpClient.Response examProgress = assessmentClient().putForm(
                    "/internal/exams/" + examId + "/progress",
                    data("studentId", student.userId, "content", "暂存答案"), internalHeaders());
            E2eHttpClient.Response examSubmit = assessmentClient().postForm(
                    "/internal/exams/" + examId + "/submit",
                    data("studentId", student.userId, "content", "正确"), internalHeaders());

            E2eHttpClient gateway = gatewayClient();
            E2eHttpClient.Response pageLogin = gateway.postForm("/login", data(
                    "username", studentUsername, "password", PASSWORD));
            E2eHttpClient.Response judge = gateway.postJson("/api/v2/judge/submit", objectData(
                    "taskId", programmingId,
                    "language", "python",
                    "code", "a,b=map(int,input().split())\nprint(a+b)"));
            E2eHttpClient.Response ai = gateway.postJson("/api/v2/ai/chat", objectData(
                    "courseName", "学生全流程课程", "message", "请总结微服务中的服务边界。"));

            E2eHttpClient.Response scores = assessmentClient().get(
                    "/internal/scores/student/" + student.userId, internalHeaders());
            E2eHttpClient.Response notifications = userClient().get(
                    "/api/notifications", bearer(studentLogin.token));
            Map<String, String> classRow = learningDb.row(
                    "SELECT current_count,max_count FROM course_class WHERE id=?",
                    new String[] {"current_count", "max_count"}, classId);
            Map<String, String> progressRow = learningDb.row(
                    "SELECT progress,last_position,duration FROM resource_progress WHERE student_id=? AND resource_id=?",
                    new String[] {"progress", "last_position", "duration"}, student.userId, resourceId);
            Map<String, String> homeworkRow = assessmentDb.row(
                    "SELECT score,status,file_path FROM submission WHERE id=?",
                    new String[] {"score", "status", "file_path"}, submissionId);
            Map<String, String> examRow = assessmentDb.row(
                    "SELECT status,score,content FROM exam_record WHERE student_id=? AND task_id=?",
                    new String[] {"status", "score", "content"}, student.userId, examId);
            Map<String, String> judgeRow = assessmentDb.row(
                    "SELECT status,judge_result,score FROM submission WHERE student_id=? AND task_id=?",
                    new String[] {"status", "judge_result", "score"}, student.userId, programmingId);

            actual.put("registration", merge("teacher", teacher.asMap(), "student", student.asMap()));
            actual.put("login", studentLogin.asMap());
            actual.put("courseAndEnrollment", merge("before", coursesBefore.summary(),
                    "course", course.asMap(), "class", clazz.asMap(), "enroll", enroll.summary(),
                    "myCourses", myCourses.summary(), "check", enrollmentCheck.summary(), "classDb", classRow));
            actual.put("resourceProgress", merge("save", progressSave.summary(),
                    "read", progressRead.summary(), "db", progressRow));
            actual.put("discussionAndNote", merge("post", post.summary(), "reply", reply.summary(), "note", note.asMap()));
            actual.put("assessment", merge("homework", graded.summary(), "homeworkDb", homeworkRow,
                    "examBegin", examBegin.summary(), "examProgress", examProgress.summary(),
                    "examSubmit", examSubmit.summary(), "examDb", examRow, "scores", scores.summary()));
            actual.put("gateway", merge("pageLogin", pageLogin.summary(), "judge", judge.summary(), "ai", ai.summary()));
            actual.put("judgeDb", judgeRow);
            actual.put("notifications", notifications.summary());

            passed = studentLogin.userId != null && !studentLogin.token.isEmpty()
                    && courseId != null && classId != null && resourceId != null
                    && apiCode(enroll) == 200 && apiCode(enrollmentCheck) == 200
                    && Boolean.TRUE.equals(apiValue(enrollmentCheck))
                    && apiCode(progressSave) == 200 && "50.00".equals(progressRow.get("progress"))
                    && postId != null && apiCode(post) == 200 && apiCode(reply) == 200 && note.id != null
                    && homeworkId != null && "graded".equals(homeworkRow.get("status"))
                    && "88.0".equals(homeworkRow.get("score"))
                    && "SUBMITTED".equals(examRow.get("status")) && "正确".equals(examRow.get("content"))
                    && apiCode(pageLogin) == 302 && apiCode(judge) == 200
                    && "AC".equals(judgeRow.get("judge_result"))
                    && apiCode(ai) == 200 && !safe(apiData(ai).get("reply")).isEmpty()
                    && apiCode(notifications) == 200;
            assertion = passed ? "通过：学生完成注册、选课、学习、讨论、作业、考试、编程、成绩和 AI 链路。"
                    : "失败：学生全流程存在异常。";
            Assertions.assertTrue(passed, assertion);
        } catch (Throwable exception) {
            actual.put("exception", exception.toString());
            assertion = "失败：" + exception.getMessage();
            rethrow(exception);
        } finally {
            E2eMatrix.add(
                    "主线一_学生：注册 -> 登录 -> 选课 -> 资源进度 -> 讨论 -> 普通作业 -> 考试 -> 编程作业 -> 成绩 -> AI",
                    "S001",
                    "Java/JUnit5: user-service + learning-service + assessment-service + gateway HTTP/JDBC",
                    objectData("teacher", teacherUsername, "student", studentUsername,
                            "courseCode", "E2E-S-" + stamp.substring(stamp.length() - 6)),
                    "三服务已启动；测试账号可以注册；学习课程、班级、资源和考核任务可写入。",
                    "学生旅程跨 user、learning、assessment 和 BFF/gateway 完成；选课、进度、成绩和判题结果可回读。",
                    actual, assertion, passed);
            cleanupCourse(learningDb, courseId);
            cleanupTasks(assessmentDb, homeworkId, examId, programmingId);
            cleanupUser(userDb, teacherUsername);
            cleanupUser(userDb, studentUsername);
            close(userDb);
            close(learningDb);
            close(assessmentDb);
        }
    }

    @Test
    @Order(3)
    void teacherMainline() throws Exception {
        String stamp = stamp();
        String suffix = stamp.substring(stamp.length() - 10);
        String teacherUsername = "e2e_tt_" + suffix;
        String studentUsername = "e2e_ts_" + suffix;
        E2eDatabase userDb = null;
        E2eDatabase learningDb = null;
        E2eDatabase assessmentDb = null;
        Map<String, Object> actual = new LinkedHashMap<>();
        boolean passed = false;
        String assertion = "失败：教师主线未完成。";
        Long courseId = null;
        Long classId = null;
        Long taskId = null;
        try {
            userDb = userDatabase();
            learningDb = learningDatabase();
            assessmentDb = assessmentDatabase();

            RegistrationResult teacher = register(userClient(), teacherUsername, "teacher", "E2E 教师主线");
            RegistrationResult student = register(userClient(), studentUsername, "student", "E2E 加入学生");
            EntityResult course = createCourse(teacher.userId, "教师课程 " + stamp,
                    "E2E-T-" + stamp.substring(stamp.length() - 6), true, "active");
            courseId = course.id;
            EntityResult clazz = createClass(courseId, "教师班级", 20);
            classId = clazz.id;
            EntityResult resource = createResource(courseId, "教师发布资源", "uploads/demo/teacher.pdf", "pdf", "第2章", 2048);

            E2eHttpClient.Response joinByInvite = learningClient().postForm("/internal/enrollments/by-invite",
                    data("studentId", student.userId, "inviteCode", safe(clazz.body.get("inviteCode"))), internalHeaders());
            taskId = createTask(courseId, "教师普通作业", "homework", "教师发布的作业", null, null).id;
            E2eHttpClient.Response studentSubmission = assessmentClient().postForm("/internal/submissions", data(
                    "taskId", taskId, "studentId", student.userId, "content", "学生提交内容"), internalHeaders());
            Long submissionId = longValue(asMap(apiValue(studentSubmission)).get("id"));
            E2eHttpClient.Response grade = assessmentClient().postForm(
                    "/internal/submissions/" + submissionId + "/grade",
                    data("score", 91, "feedback", "教师复核通过"), internalHeaders());
            E2eHttpClient.Response teacherTasks = assessmentClient().get(
                    "/internal/tasks?courseId=" + courseId, internalHeaders());
            E2eHttpClient.Response stats = assessmentClient().get(
                    "/internal/scores/course/" + courseId, internalHeaders());
            E2eHttpClient.Response wrongUpdate = learningClient().putForm("/api/courses/" + courseId, data(
                    "teacherId", teacher.userId + 999, "name", "越权更新"), internalHeaders());
            E2eHttpClient.Response archive = learningClient().delete(
                    "/api/courses/" + courseId + "?teacherId=" + teacher.userId, internalHeaders());
            Map<String, String> courseRow = learningDb.row(
                    "SELECT status FROM course WHERE id=?", new String[] {"status"}, courseId);
            Map<String, String> classRow = learningDb.row(
                    "SELECT current_count,max_count FROM course_class WHERE id=?",
                    new String[] {"current_count", "max_count"}, classId);
            Map<String, String> submissionRow = assessmentDb.row(
                    "SELECT score,status FROM submission WHERE id=?", new String[] {"score", "status"}, submissionId);
            String notificationCount = userDb.scalar(
                    "SELECT COUNT(*) FROM notification WHERE user_id=? AND title='成绩已发布'", student.userId);

            actual.put("registration", merge("teacher", teacher.asMap(), "student", student.asMap()));
            actual.put("course", course.asMap());
            actual.put("class", clazz.asMap());
            actual.put("resource", resource.asMap());
            actual.put("joinByInvite", joinByInvite.summary());
            actual.put("assessment", merge("task", taskId, "submission", studentSubmission.summary(),
                    "grade", grade.summary(), "tasks", teacherTasks.summary(), "stats", stats.summary()));
            actual.put("authorization", merge("wrongUpdate", wrongUpdate.summary(), "archive", archive.summary()));
            actual.put("database", merge("course", courseRow, "class", classRow,
                    "submission", submissionRow, "gradeNotificationCount", notificationCount));

            passed = courseId != null && classId != null && taskId != null
                    && apiCode(joinByInvite) == 200 && apiCode(studentSubmission) == 200
                    && apiCode(grade) == 200 && "91.0".equals(submissionRow.get("score"))
                    && "graded".equals(submissionRow.get("status")) && Integer.parseInt(notificationCount) > 0
                    && apiCode(teacherTasks) == 200 && apiCode(stats) == 200
                    && apiCode(wrongUpdate) == 403 && apiCode(archive) == 200
                    && "archived".equals(courseRow.get("status"));
            assertion = passed ? "通过：教师完成课程、班级、资源、作业、批改、统计和归档链路。"
                    : "失败：教师全流程存在异常。";
            Assertions.assertTrue(passed, assertion);
        } catch (Throwable exception) {
            actual.put("exception", exception.toString());
            assertion = "失败：" + exception.getMessage();
            rethrow(exception);
        } finally {
            E2eMatrix.add(
                    "主线二_教师：注册 -> 登录 -> 创建课程/班级 -> 发布资源/作业 -> 批改复核 -> 成绩统计 -> 归档",
                    "T001",
                    "Java/JUnit5: learning-service + assessment-service internal HTTP/JDBC",
                    objectData("teacher", teacherUsername, "student", studentUsername,
                            "courseCode", "E2E-T-" + stamp.substring(stamp.length() - 6)),
                    "教师和学生账号可用；课程、班级、资源及考核服务可写入；学生可通过邀请码加入。",
                    "课程内容生产、学生加入、作业批改、通知、统计和有学生时归档均成功；越权更新返回 403。",
                    actual, assertion, passed);
            cleanupCourse(learningDb, courseId);
            cleanupTasks(assessmentDb, taskId);
            cleanupUser(userDb, teacherUsername);
            cleanupUser(userDb, studentUsername);
            close(userDb);
            close(learningDb);
            close(assessmentDb);
        }
    }

    @Test
    @Order(4)
    void adminMainline() throws Exception {
        String stamp = stamp();
        String username = "e2e_as_" + stamp.substring(stamp.length() - 10);
        E2eDatabase userDb = null;
        Map<String, Object> actual = new LinkedHashMap<>();
        boolean passed = false;
        String assertion = "失败：管理员主线未完成。";
        try {
            userDb = userDatabase();
            String adminUsername = findAdminUsername(userDb);
            AuthResult admin = login(userClient(), adminUsername, PASSWORD);
            RegistrationResult target = register(userClient(), username, "student", "E2E 待治理用户");

            E2eHttpClient.Response list = userClient().get("/api/users?role=student", bearer(admin.token));
            E2eHttpClient.Response update = userClient().putJson("/internal/bff/users/" + target.userId + "/admin",
                    objectData("name", "E2E 教师角色", "email", "e2e@example.com", "role", "teacher"), internalHeaders());
            AuthResult roleLogin = login(userClient(), username, PASSWORD);
            E2eHttpClient.Response logSeed = userClient().postForm("/internal/operation-logs",
                    data("userId", target.userId, "username", username, "action", "E2E 管理审计", "detail", "角色已修改"), internalHeaders());
            E2eHttpClient.Response logs = userClient().get("/api/logs", bearer(admin.token));
            E2eHttpClient.Response disable = userClient().putForm(
                    "/api/users/" + target.userId + "/status?status=0", Collections.emptyMap(), bearer(admin.token));
            E2eHttpClient.Response disabledLogin = userClient().postJson("/api/auth/login",
                    objectData("username", username, "password", PASSWORD));
            E2eHttpClient.Response reset = userClient().postForm(
                    "/api/users/" + target.userId + "/reset-password",
                    data("password", "654321"), bearer(admin.token));
            E2eHttpClient.Response enable = userClient().putForm(
                    "/api/users/" + target.userId + "/status?status=1", Collections.emptyMap(), bearer(admin.token));
            E2eHttpClient.Response newLogin = userClient().postJson("/api/auth/login",
                    objectData("username", username, "password", "654321"));
            E2eHttpClient.Response delete = userClient().delete("/api/users/" + target.userId, bearer(admin.token));
            String remaining = userDb.scalar("SELECT COUNT(*) FROM `user` WHERE username=?", username);
            String adminStillExists = userDb.scalar(
                    "SELECT COUNT(*) FROM `user` WHERE username=? AND role='admin' AND status=1", adminUsername);

            actual.put("adminLogin", admin.response.summary());
            actual.put("targetRegistration", target.asMap());
            actual.put("list", list.summary());
            actual.put("roleUpdate", merge("response", update.summary(), "loginUser", roleLogin.asMap()));
            actual.put("audit", merge("seed", logSeed.summary(), "logs", logs.summary()));
            actual.put("passwordAndStatus", merge("disable", disable.summary(),
                    "disabledLogin", disabledLogin.summary(), "reset", reset.summary(),
                    "enable", enable.summary(), "newLogin", newLogin.summary()));
            actual.put("delete", delete.summary());
            actual.put("database", merge("remainingTarget", remaining, "adminStillExists", adminStillExists));

            passed = apiCode(admin.response) == 200 && apiCode(list) == 200
                    && apiCode(update) == 200 && "teacher".equals(roleLogin.user.get("role"))
                    && apiCode(logSeed) == 200 && apiCode(logs) == 200
                    && apiCode(disable) == 200 && apiCode(disabledLogin) == 401
                    && apiCode(reset) == 200 && apiCode(enable) == 200
                    && apiCode(newLogin) == 200 && apiCode(delete) == 200
                    && "0".equals(remaining) && "1".equals(adminStillExists);
            assertion = passed ? "通过：管理员完成用户查询、角色/资料修改、审计、禁用、重置密码和删除。"
                    : "失败：管理员治理主线存在异常。";
            Assertions.assertTrue(passed, assertion);
        } catch (Throwable exception) {
            actual.put("exception", exception.toString());
            assertion = "失败：" + exception.getMessage();
            rethrow(exception);
        } finally {
            E2eMatrix.add(
                    "主线三_管理员：登录 -> 查询用户 -> 修改资料/角色 -> 审计日志 -> 禁用 -> 重置密码 -> 删除",
                    "A001",
                    "Java/JUnit5: user-service public/internal HTTP/JDBC",
                    objectData("admin", "seeded admin", "temporaryUser", username,
                            "resetPassword", "654321"),
                    "user-service 已启动；存在启用中的管理员账号；测试用户可注册。",
                    "管理员可治理普通用户；普通用户被禁用后不能登录；重置密码后可登录；测试用户删除且管理员账号保留。",
                    actual, assertion, passed);
            cleanupUser(userDb, username);
            close(userDb);
        }
    }

    @Test
    @Order(5)
    void faultToleranceMainline() throws Exception {
        String stamp = stamp();
        String suffix = stamp.substring(stamp.length() - 10);
        String teacherUsername = "e2e_ft_" + suffix;
        String studentUsername = "e2e_fs_" + suffix;
        E2eDatabase userDb = null;
        E2eDatabase learningDb = null;
        E2eDatabase assessmentDb = null;
        Map<String, Object> actual = new LinkedHashMap<>();
        boolean passed = false;
        String assertion = "失败：异常与降级主线未完成。";
        Long courseId = null;
        Long closedCourseId = null;
        Long classId = null;
        Long taskId = null;
        try {
            userDb = userDatabase();
            learningDb = learningDatabase();
            assessmentDb = assessmentDatabase();
            RegistrationResult teacher = register(userClient(), teacherUsername, "teacher", "E2E 容错教师");
            RegistrationResult student = register(userClient(), studentUsername, "student", "E2E 容错学生");
            EntityResult course = createCourse(teacher.userId, "容错课程 " + stamp,
                    "E2E-F-" + stamp.substring(stamp.length() - 6), true, "active");
            courseId = course.id;
            EntityResult clazz = createClass(courseId, "最后一个名额", 1);
            classId = clazz.id;
            taskId = createTask(courseId, "容错编程题", "programming",
                    metadata("输出两个整数的和。", null,
                            "---CASE---\n2 3\n---OUTPUT---\n5", "python"), null, null).id;

            E2eHttpClient.Response firstEnroll = learningClient().postForm("/api/enrollments",
                    data("studentId", student.userId, "courseId", courseId, "classId", classId));
            E2eHttpClient.Response duplicateEnroll = learningClient().postForm("/api/enrollments",
                    data("studentId", student.userId, "courseId", courseId, "classId", classId));
            String enrollmentCount = learningDb.scalar(
                    "SELECT COUNT(*) FROM course_enrollment WHERE course_id=?", courseId);
            String classCount = learningDb.scalar(
                    "SELECT current_count FROM course_class WHERE id=?", classId);

            E2eHttpClient gateway = gatewayClient();
            E2eHttpClient.Response pageLogin = gateway.postForm("/login", data(
                    "username", studentUsername, "password", PASSWORD));
            E2eHttpClient.Response localJudge = gateway.postJson("/api/v2/judge/submit", objectData(
                    "taskId", taskId, "language", "python",
                    "code", "a,b=map(int,input().split())\nprint(a+b)"));
            E2eHttpClient.Response aiFallback = gateway.postJson("/api/v2/ai/chat", objectData(
                    "courseName", "容错课程", "message", "请回答一个不依赖外部密钥的问题。"));

            E2eHttpClient.Response gradeSeed = assessmentClient().postForm("/internal/submissions",
                    data("taskId", taskId, "studentId", student.userId, "content", "容错提交"), internalHeaders());
            Long gradeSeedId = longValue(asMap(apiValue(gradeSeed)).get("id"));
            E2eHttpClient.Response gradeWithNotificationFailure = assessmentClient().postForm(
                    "/internal/submissions/" + gradeSeedId + "/grade",
                    data("score", 77, "feedback", "通知服务异常时仍应保留成绩"), internalHeaders());
            Map<String, String> savedGrade = assessmentDb.row(
                    "SELECT score,status FROM submission WHERE id=?",
                    new String[] {"score", "status"}, gradeSeedId);

            E2eHttpClient.Response unauthorized = userClient().get("/api/profile");
            E2eHttpClient.Response closedCourse = learningClient().postForm("/api/courses", data(
                    "teacherId", teacher.userId, "name", "关闭课程 " + stamp,
                    "code", "E2E-FC-" + stamp.substring(stamp.length() - 6),
                    "allowJoin", false, "status", "closed"));
            closedCourseId = longValue(asMap(apiValue(closedCourse)).get("id"));
            Long closedClassId = createClass(closedCourseId, "关闭班级", 10).id;
            E2eHttpClient.Response closedEnroll = learningClient().postForm("/api/enrollments", data(
                    "studentId", student.userId, "courseId", closedCourseId, "classId", closedClassId));
            String closedCount = learningDb.scalar(
                    "SELECT COUNT(*) FROM course_enrollment WHERE student_id=? AND course_id=?",
                    student.userId, closedCourseId);

            actual.put("enrollmentIdempotency", merge("first", firstEnroll.summary(),
                    "duplicate", duplicateEnroll.summary(), "enrollmentCount", enrollmentCount,
                    "classCount", classCount));
            actual.put("judgeFallback", merge("login", pageLogin.summary(), "judge", localJudge.summary()));
            actual.put("aiFallback", aiFallback.summary());
            actual.put("notificationFailure", merge("gradeSeed", gradeSeed.summary(),
                    "grade", gradeWithNotificationFailure.summary(), "savedGrade", savedGrade));
            actual.put("authorizationAndClosedCourse", merge("unauthorized", unauthorized.summary(),
                    "closedCourse", closedCourse.summary(), "closedEnroll", closedEnroll.summary(),
                    "closedCount", closedCount));

            passed = apiCode(firstEnroll) == 200 && apiCode(duplicateEnroll) == 200
                    && "1".equals(enrollmentCount) && "1".equals(classCount)
                    && apiCode(pageLogin) == 302 && apiCode(localJudge) == 200
                    && Boolean.TRUE.equals(apiData(localJudge).get("usedLocalJudge"))
                    && "AC".equals(apiData(localJudge).get("status"))
                    && apiCode(aiFallback) == 200 && !safe(apiData(aiFallback).get("reply")).isEmpty()
                    && apiCode(gradeWithNotificationFailure) == 200
                    && "77.0".equals(savedGrade.get("score")) && "graded".equals(savedGrade.get("status"))
                    && unauthorized.status == 401 && apiCode(closedEnroll) == 400
                    && "0".equals(closedCount);
            assertion = passed ? "通过：重复选课幂等、Judge0 本地降级、AI 降级、通知失败不阻断成绩和权限边界均通过。"
                    : "失败：异常与降级主线存在异常；容量上限仅记录当前实现的实际行为。";
            Assertions.assertTrue(passed, assertion);
        } catch (Throwable exception) {
            actual.put("exception", exception.toString());
            assertion = "失败：" + exception.getMessage();
            rethrow(exception);
        } finally {
            E2eMatrix.add(
                    "主线四_跨服务异常与降级：重复选课/关闭课程/Judge0/AI/批改通知/未登录",
                    "F001",
                    "Java/JUnit5: gateway + three services HTTP/JDBC",
                    objectData("judge0", "127.0.0.1:9", "aiApiKey", "empty",
                            "classMaxCount", 1, "capacityNote", "当前实现未强制 max_count 上限"),
                    "三服务和 gateway 已启动；Judge0 被配置为不可达且本地降级开启；AI 未配置密钥。",
                    "重复选课不新增记录；关闭课程拒绝选课；判题和 AI 返回降级结果；通知失败不影响成绩保存；未登录返回 401。",
                    actual, assertion, passed);
            cleanupCourse(learningDb, courseId);
            cleanupCourse(learningDb, closedCourseId);
            cleanupTasks(assessmentDb, taskId);
            cleanupUser(userDb, teacherUsername);
            cleanupUser(userDb, studentUsername);
            close(userDb);
            close(learningDb);
            close(assessmentDb);
        }
    }

    private RegistrationResult register(E2eHttpClient client, String username, String role, String name) throws Exception {
        E2eHttpClient.Response response = client.postJson("/api/auth/register", objectData(
                "username", username, "password", PASSWORD, "role", role, "name", name));
        Map<String, Object> user = asMap(apiValue(response));
        return new RegistrationResult(response, longValue(user.get("id")), user);
    }

    private AuthResult login(E2eHttpClient client, String username, String password) throws Exception {
        E2eHttpClient.Response response = client.postJson("/api/auth/login", objectData(
                "username", username, "password", password));
        Map<String, Object> data = apiData(response);
        Map<String, Object> user = asMap(data.get("user"));
        return new AuthResult(response, safe(data.get("token")), longValue(user.get("id")), user);
    }

    private EntityResult createCourse(Long teacherId, String name, String code, boolean allowJoin, String status) throws Exception {
        E2eHttpClient.Response response = learningClient().postForm("/api/courses", data(
                "teacherId", teacherId, "name", name, "code", code,
                "description", name + " 描述", "credits", 3,
                "subjectCategory", "微服务 E2E", "hours", 32,
                "allowJoin", allowJoin, "status", status));
        return entity(response);
    }

    private EntityResult createClass(Long courseId, String name, int maxCount) throws Exception {
        E2eHttpClient.Response response = learningClient().postForm("/api/classes", data(
                "courseId", courseId, "name", name, "maxCount", maxCount));
        return entity(response);
    }

    private EntityResult createResource(Long courseId, String title, String filePath,
                                        String type, String chapter, long fileSize) throws Exception {
        E2eHttpClient.Response response = learningClient().postJson("/internal/bff/resources",
                objectData("courseId", courseId, "title", title, "filePath", filePath,
                        "type", type, "chapter", chapter, "fileSize", fileSize), internalHeaders());
        return entity(response);
    }

    private EntityResult createNote(Long studentId, Long courseId, Long resourceId,
                                    String title, String content) throws Exception {
        E2eHttpClient.Response response = learningClient().postJson("/internal/bff/notes",
                objectData("studentId", studentId, "courseId", courseId, "resourceId", resourceId,
                        "title", title, "content", content), internalHeaders());
        return entity(response);
    }

    private EntityResult createTask(Long courseId, String title, String type, String description,
                                    String codeTemplate, Integer maxScore) throws Exception {
        Map<String, Object> task = objectData(
                "courseId", courseId, "title", title, "type", type,
                "description", description, "maxScore", maxScore == null ? 100 : maxScore,
                "timeLimitMs", 15000, "memoryLimitMb", 128,
                "codeTemplate", codeTemplate, "status", "published");
        E2eHttpClient.Response response = assessmentClient().postJson("/internal/tasks", task, internalHeaders());
        return entity(response);
    }

    private EntityResult entity(E2eHttpClient.Response response) {
        return new EntityResult(response, longValue(asMap(apiValue(response)).get("id")),
                asMap(apiValue(response)));
    }

    private String metadata(String visible, String examAnswer, String testCases, String language) {
        StringBuilder result = new StringBuilder(visible == null ? "" : visible);
        result.append("\n\n<!--TP_META\n");
        appendMeta(result, "examAnswer", examAnswer);
        appendMeta(result, "testCases", testCases);
        appendMeta(result, "allowedLanguage", language);
        result.append("TP_META-->");
        return result.toString();
    }

    private void appendMeta(StringBuilder result, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            result.append(key).append('=').append(Base64.getEncoder().encodeToString(
                    value.getBytes(StandardCharsets.UTF_8))).append('\n');
        }
    }

    private String findAdminUsername(E2eDatabase database) throws SQLException {
        String username = database.scalar(
                "SELECT username FROM `user` WHERE role='admin' AND status=1 ORDER BY id LIMIT 1");
        if (username == null) throw new SQLException("没有可用的启用管理员账号");
        return username;
    }

    private void cleanupTasks(E2eDatabase database, Long... ids) {
        if (database == null) return;
        for (Long id : ids) {
            if (id == null) continue;
            try {
                database.execute("DELETE FROM task WHERE id=?", id);
            } catch (SQLException ignored) {
            }
        }
    }

    private void rethrow(Throwable exception) throws Exception {
        if (exception instanceof Error) throw (Error) exception;
        if (exception instanceof Exception) throw (Exception) exception;
        throw new Exception(exception);
    }

    private void close(AutoCloseable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    private static final class RegistrationResult {
        final E2eHttpClient.Response response;
        final Long userId;
        final Map<String, Object> user;

        RegistrationResult(E2eHttpClient.Response response, Long userId, Map<String, Object> user) {
            this.response = response;
            this.userId = userId;
            this.user = user;
        }

        Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("response", response.summary());
            result.put("userId", userId);
            result.put("user", user);
            return result;
        }
    }

    private static final class AuthResult {
        final E2eHttpClient.Response response;
        final String token;
        final Long userId;
        final Map<String, Object> user;

        AuthResult(E2eHttpClient.Response response, String token, Long userId, Map<String, Object> user) {
            this.response = response;
            this.token = token;
            this.userId = userId;
            this.user = user;
        }

        Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("response", response.summary());
            result.put("tokenPresent", token != null && !token.isEmpty());
            result.put("userId", userId);
            result.put("user", user);
            return result;
        }
    }

    private static final class EntityResult {
        final E2eHttpClient.Response response;
        final Long id;
        final Map<String, Object> body;

        EntityResult(E2eHttpClient.Response response, Long id, Map<String, Object> body) {
            this.response = response;
            this.id = id;
            this.body = body == null ? new LinkedHashMap<>() : body;
        }

        Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("response", response.summary());
            result.put("id", id);
            result.put("body", body);
            return result;
        }
    }
}
