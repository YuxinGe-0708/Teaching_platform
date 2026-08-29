package org.example.e2e;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

abstract class E2eTestSupport {
    static final ObjectMapper JSON = new ObjectMapper();
    static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    protected String stamp() {
        return LocalDateTime.now().format(STAMP) + UUID.randomUUID().toString().substring(0, 4);
    }

    protected E2eHttpClient client() {
        return new E2eHttpClient(E2eConfig.BASE_URL);
    }

    protected E2eDatabase database() throws SQLException {
        return E2eDatabase.connect();
    }

    protected Map<String, String> data(Object... values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), String.valueOf(values[i + 1]));
        return result;
    }

    protected Map<String, Object> objectData(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }

    protected Map<String, Object> jsonObject(String body) {
        try {
            return JSON.readValue(body, new TypeReference<Map<String, Object>>() { });
        } catch (Exception exception) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("raw", body.substring(0, Math.min(500, body.length())));
            return result;
        }
    }

    protected Map<String, String> row(E2eDatabase db, String sql, String... columns) throws SQLException {
        return db.row(sql, columns);
    }

    protected String id(E2eDatabase db, String sql, Object... parameters) throws SQLException {
        String value = db.scalar(sql, parameters);
        Assertions.assertFalse(value == null || value.trim().isEmpty(), "数据库未找到 ID，SQL: " + sql);
        return value;
    }

    protected E2eHttpClient.Response login(E2eHttpClient client, String username, String password) throws IOException {
        client.get("/login");
        return client.postForm("/login", data("username", username, "password", password));
    }

    protected E2eHttpClient.Response register(E2eHttpClient client, String username, String role, E2eDatabase db) throws Exception {
        E2eHttpClient.Response response = client.postForm("/register", data("username", username, "password", "123456", "role", role));
        String userId = id(db, "SELECT id FROM `user` WHERE username=?", username);
        response.headers.put("X-E2E-User-Id", userId);
        return response;
    }

    protected String userId(E2eDatabase db, String username) throws SQLException {
        return id(db, "SELECT id FROM `user` WHERE username=?", username);
    }

    protected Map<String, Object> createCourse(E2eHttpClient teacher, E2eDatabase db, String prefix, String stamp,
                                               String status, String allowJoin) throws Exception {
        String name = prefix + "课程" + stamp;
        String code = prefix.toUpperCase() + stamp.substring(Math.max(0, stamp.length() - 8));
        E2eHttpClient.Response response = teacher.postForm("/teacher/course/create", data(
                "courseName", name, "courseCode", code, "credit", "3", "subjectCategory", "软件测试",
                "hours", "32", "allowJoin", allowJoin, "status", status, "description", prefix + " E2E 测试课程"));
        String lookup = "SELECT id FROM course WHERE code=? ORDER BY id DESC LIMIT 1";
        String courseId = id(db, lookup, code);
        return objectData("name", name, "code", code, "id", courseId, "lookupSql", lookup, "response", response.summary());
    }

    protected Map<String, Object> createClass(E2eHttpClient teacher, E2eDatabase db, String courseId,
                                               String name, int maxCount) throws Exception {
        E2eHttpClient.Response response = teacher.postForm("/teacher/course/class/create", data(
                "courseId", courseId, "className", name, "maxCount", maxCount));
        String lookup = "SELECT id FROM course_class WHERE course_id=? AND name=? ORDER BY id DESC LIMIT 1";
        String classId = id(db, lookup, courseId, name);
        return objectData("id", classId, "lookupSql", lookup, "response", response.summary());
    }

    protected Map<String, Object> createTask(E2eHttpClient teacher, E2eDatabase db, String courseId,
                                              String title, String type, Map<String, ?> extra) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("courseId", courseId);
        request.put("title", title);
        request.put("taskType", type);
        request.put("content", title + " 内容");
        request.put("endTime", "2037-12-31T23:59");
        request.put("status", "published");
        request.put("fullScore", "100");
        request.put("timeLimitMs", "10000");
        request.put("memoryLimitMb", "128");
        if (extra != null) request.putAll(extra);
        E2eHttpClient.Response response = teacher.postForm("/teacher/task/create", request);
        String lookup = "SELECT id FROM task WHERE course_id=? AND title=? ORDER BY id DESC LIMIT 1";
        String taskId = id(db, lookup, courseId, title);
        return objectData("id", taskId, "title", title, "type", type, "lookupSql", lookup, "response", response.summary());
    }

    protected Map<String, Object> upload(E2eHttpClient teacher, E2eDatabase db, String courseId,
                                         String title, Path file) throws Exception {
        E2eHttpClient.Response response = teacher.postMultipart("/teacher/resource/upload",
                data("courseId", courseId, "title", title, "chapter", "E2E章节"), "file", file);
        String lookup = "SELECT id FROM resource WHERE course_id=? AND title=? ORDER BY id DESC LIMIT 1";
        String resourceId = id(db, lookup, courseId, title);
        return objectData("id", resourceId, "title", title, "lookupSql", lookup, "response", response.summary());
    }

    protected Path requireFile(Path path) {
        Assertions.assertTrue(Files.exists(path), "测试文件不存在: " + path);
        return path;
    }

    protected void assertRedirect(E2eHttpClient.Response response, String path) {
        Assertions.assertTrue(response.status == 301 || response.status == 302 || response.status == 303,
                "期望重定向到 " + path + "，实际: " + response.summary());
        Assertions.assertTrue(response.location().contains(path), "期望 Location 包含 " + path + "，实际: " + response.location());
    }

    protected void assertPage(E2eHttpClient.Response response, String path) {
        Assertions.assertEquals(200, response.status, path + " 返回异常: " + response.summary());
        Assertions.assertFalse(response.body().contains("Whitelabel Error Page"), path + " 渲染错误页面");
        Assertions.assertFalse(response.body().contains("Internal Server Error"), path + " 渲染内部错误页面");
    }

    protected Map<String, Object> http(String label, E2eHttpClient.Response response) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(label, response.summary());
        return result;
    }

    protected Map<String, Object> merge(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }

    protected void cleanupCourse(E2eDatabase db, String courseId) {
        if (db == null || courseId == null || courseId.trim().isEmpty()) return;
        try {
            List<List<String>> paths = db.query("SELECT file_path FROM resource WHERE course_id=?", courseId);
            for (List<String> path : paths) cleanupUpload(path.isEmpty() ? "" : path.get(0));
            db.execute("DELETE FROM course WHERE id=?", courseId);
        } catch (Exception exception) {
            System.err.println("E2E course cleanup incomplete: " + exception.getMessage());
        }
    }

    protected void cleanupUser(E2eDatabase db, String username) {
        if (db == null || username == null || username.trim().isEmpty()) return;
        try {
            db.execute("DELETE FROM `user` WHERE username=?", username);
        } catch (Exception exception) {
            System.err.println("E2E user cleanup incomplete: " + exception.getMessage());
        }
    }

    protected void cleanupUpload(String storedPath) {
        if (storedPath == null || storedPath.trim().isEmpty()) return;
        String portable = storedPath.replace('\\', '/');
        int marker = portable.indexOf("uploads/");
        if (marker >= 0) portable = portable.substring(marker + "uploads/".length());
        Path root = E2eConfig.ROOT.resolve("uploads").toAbsolutePath().normalize();
        Path candidate = root.resolve(portable).normalize();
        if (candidate.startsWith(root)) {
            try { Files.deleteIfExists(candidate); } catch (IOException ignored) { }
        }
    }

    protected Path createPdfFixture() throws IOException {
        if (Files.exists(E2eConfig.PDF_PATH)) return E2eConfig.PDF_PATH;
        Path file = Files.createTempFile("teaching-platform-e2e-", ".pdf");
        Files.write(file, "%PDF-1.4\n% E2E fixture\n".getBytes(StandardCharsets.US_ASCII));
        return file;
    }

    protected Path createVideoFixture() throws IOException {
        if (Files.exists(E2eConfig.VIDEO_PATH)) return E2eConfig.VIDEO_PATH;
        Path file = Files.createTempFile("teaching-platform-e2e-", ".mp4");
        Files.write(file, new byte[] {0, 0, 0, 24, 102, 116, 121, 112, 105, 115, 111, 109});
        return file;
    }

    protected void deleteTempFixture(Path file) {
        if (file == null || file.equals(E2eConfig.PDF_PATH) || file.equals(E2eConfig.VIDEO_PATH)) return;
        try { Files.deleteIfExists(file); } catch (IOException ignored) { }
    }

    protected Map<String, Object> pageSet(E2eHttpClient client, String... paths) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String path : paths) {
            E2eHttpClient.Response response = client.get(path);
            assertPage(response, path);
            result.put(path, response.summary());
        }
        return result;
    }

    protected String jsonString(Object value) throws IOException {
        return JSON.writeValueAsString(value);
    }

    protected boolean bodyContains(E2eHttpClient.Response response, String text) {
        return response.body().contains(text);
    }

    protected String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
