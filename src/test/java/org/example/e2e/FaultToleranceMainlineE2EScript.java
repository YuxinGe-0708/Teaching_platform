package org.example.e2e;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FaultToleranceMainlineE2EScript extends E2eTestSupport {
    @Test
    void faultToleranceMainline() throws Exception {
        String stamp = stamp();
        E2eHttpClient teacher = client();
        E2eDatabase db = null;
        String courseId = "";
        String closedCourseId = "";
        List<String> usernames = new ArrayList<>();
        Map<String, Object> actual = new LinkedHashMap<>();
        boolean passed = false;
        String assertion = "失败：异常与降级主线未完成。";
        try {
            db = database();
            assertRedirect(login(teacher, "teacher_demo", "123456"), "/");
            Map<String, Object> course = createCourse(teacher, db, "F4", stamp, "active", "true");
            courseId = String.valueOf(course.get("id"));
            String classId = id(db, "SELECT id FROM course_class WHERE course_id=? ORDER BY id LIMIT 1", courseId);
            db.execute("UPDATE course_class SET max_count=1,current_count=0 WHERE id=?", classId);

            List<E2eHttpClient> students = new ArrayList<>();
            List<String> registrationIds = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                String username = "f4_" + i + "_" + stamp.substring(Math.max(0, stamp.length() - 8));
                usernames.add(username);
                E2eHttpClient student = client();
                E2eHttpClient.Response registration = register(student, username, "student", db);
                assertRedirect(registration, "registered=1");
                assertRedirect(login(student, username, "123456"), "/");
                students.add(student);
                registrationIds.add(userId(db, username));
            }

            E2eHttpClient.Response firstSelect = students.get(0).postForm("/student/course/select", data("courseId", courseId));
            E2eHttpClient.Response duplicateSelect = students.get(0).postForm("/student/course/select", data("courseId", courseId));
            String duplicateCount = db.scalar("SELECT COUNT(*) FROM course_enrollment WHERE student_id=? AND course_id=?", registrationIds.get(0), courseId);

            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<Future<E2eHttpClient.Response>> futures = new ArrayList<>();
            final String concurrentCourseId = courseId;
            try {
                for (int i = 1; i < students.size(); i++) {
                    final E2eHttpClient student = students.get(i);
                    futures.add(executor.submit(new Callable<E2eHttpClient.Response>() {
                        @Override
                        public E2eHttpClient.Response call() throws Exception {
                            return student.postForm("/student/course/select", data("courseId", concurrentCourseId));
                        }
                    }));
                }
            } finally {
                executor.shutdown();
            }
            List<Map<String, Object>> concurrentResponses = new ArrayList<>();
            for (Future<E2eHttpClient.Response> future : futures) concurrentResponses.add(future.get().summary());
            String totalEnrollments = db.scalar("SELECT COUNT(*) FROM course_enrollment WHERE course_id=?", courseId);
            Map<String, String> classRow = db.row("SELECT max_count,current_count FROM course_class WHERE id=?",
                    new String[] {"maxCount", "currentCount"}, classId);

            Map<String, Object> closedCourse = createCourse(teacher, db, "F4C", stamp, "closed", "false");
            closedCourseId = String.valueOf(closedCourse.get("id"));
            E2eHttpClient.Response closedSelect = students.get(0).postForm("/student/course/select", data("courseId", closedCourseId));
            String closedCount = db.scalar("SELECT COUNT(*) FROM course_enrollment WHERE student_id=? AND course_id=?", registrationIds.get(0), closedCourseId);

            E2eHttpClient.Response ai = students.get(0).postJson("/api/v2/ai/chat", objectData(
                    "courseId", courseId, "courseName", course.get("name"), "message", "触发AI降级检查"));
            Map<String, Object> aiJson = jsonObject(ai.body());
            Map<String, Object> aiData = aiJson.get("data") instanceof Map
                    ? (Map<String, Object>) aiJson.get("data") : new LinkedHashMap<String, Object>();

            Map<String, Object> programming = createTask(teacher, db, courseId, "F4编程降级检查" + stamp, "programming",
                    objectData("allowedLanguage", "python", "testCases", "---CASE---\n1 2\n---OUTPUT---\n3\n---WEIGHT---\n1"));
            E2eHttpClient.Response judge = students.get(0).postJson("/api/v2/judge/submit", objectData(
                    "taskId", programming.get("id"), "language", "python", "code", "a,b=map(int,input().split())\nprint(a+b)"));
            Map<String, Object> judgeJson = jsonObject(judge.body());
            Map<String, Object> judgeData = judgeJson.get("data") instanceof Map
                    ? (Map<String, Object>) judgeJson.get("data") : new LinkedHashMap<String, Object>();

            actual.put("duplicateEnrollment", merge("firstSelect", firstSelect.summary(), "duplicateSelect", duplicateSelect.summary(), "studentCourseCount", duplicateCount));
            actual.put("capacityLimit", merge("setup", merge("courseId", courseId, "classId", classId, "maxCount", classRow.get("maxCount")),
                    "concurrentResponses", concurrentResponses, "totalEnrollments", totalEnrollments, "classCount", classRow,
                    "requirementExpected", "仅 1 人成功"));
            actual.put("closedCourseRejection", merge("course", closedCourse, "select", closedSelect.summary(), "enrollmentCount", closedCount));
            actual.put("aiDegradation", merge("http", ai.summary(), "response", aiJson, "reply", aiData.get("reply")));
            actual.put("judge0Fallback", merge("http", judge.summary(), "response", judgeJson,
                    "usedLocalJudge", judgeData.get("usedLocalJudge"), "note", "usedLocalJudge=true 才表示本次触发本地判题降级；false 表示 Judge0 可用。"));
            actual.put("unsupportedFaultInjection", new String[] {
                    "当前项目不是拆分部署的 user-service，无法仅通过该测试类单独 Mock user-service 超时。",
                    "当前批改流程未提供可控通知服务调用，无法直接注入通知失败。"
            });

            boolean duplicateOk = "1".equals(duplicateCount);
            boolean capacityOk = "1".equals(totalEnrollments) && "1".equals(classRow.get("currentCount"));
            boolean closedOk = "0".equals(closedCount);
            boolean aiOk = ai.status == 200 && safe(aiData.get("reply")).length() > 0;
            boolean judgeOk = judge.status == 200 && "200".equals(String.valueOf(judgeJson.get("code")));
            passed = duplicateOk && capacityOk && closedOk && aiOk && judgeOk;
            assertion = passed ? "通过：异常与降级容错主线通过。" : "部分通过/发现缺口：容量上限或强制故障降级未满足预期，详见实际输出。";
            if (!capacityOk) assertion += " 当前实现的容量上限控制未生效。";
            if (!Boolean.TRUE.equals(judgeData.get("usedLocalJudge"))) assertion += " 本次未触发 Judge0 本地回退。";
            org.junit.jupiter.api.Assertions.assertTrue(passed, assertion);
        } catch (Throwable exception) {
            actual.put("exception", exception.toString());
            assertion = "失败：" + exception.getMessage();
            if (exception instanceof Error) throw (Error) exception;
            throw (Exception) exception;
        } finally {
            E2eMatrix.add(
                    "主线四_跨服务异常与降级容错闭环：选课/提交作业/批改通知/AI调用 -> 依赖异常 -> 降级处理",
                    "F010",
                    "CourseService.enroll; StudentController.selectCourse; AiController.chat; JudgeController.submitAndJudge",
                    objectData("duplicateEnrollment", "同一学生连续选课", "concurrentEnrollment", objectData("capacity", 1, "students", 10),
                            "closedCourse", objectData("status", "closed", "allowJoin", false), "judgeFallback", "观察 usedLocalJudge", "aiMessage", "触发AI降级检查"),
                    "容器 frontend/backend/mysql 已启动；teacher_demo 可登录；数据库允许设置班级容量；AI/Judge0 配置可观测。",
                    "重复选课不重复；并发选课不超过班级容量；关闭课程拒绝选课；AI/Judge0 依赖异常时返回明确降级结果。",
                    actual,
                    assertion,
                    passed);
            cleanupCourse(db, courseId);
            cleanupCourse(db, closedCourseId);
            for (String username : usernames) cleanupUser(db, username);
            if (db != null) db.close();
        }
    }
}
