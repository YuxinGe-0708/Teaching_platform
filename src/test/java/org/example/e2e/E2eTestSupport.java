package org.example.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class E2eTestSupport {
    private static final ObjectMapper JSON = new ObjectMapper();

    protected E2eHttpClient userClient() {
        return new E2eHttpClient(E2eConfig.USER_SERVICE_BASE_URL);
    }

    protected E2eHttpClient learningClient() {
        return new E2eHttpClient(E2eConfig.LEARNING_SERVICE_BASE_URL);
    }

    protected E2eHttpClient assessmentClient() {
        return new E2eHttpClient(E2eConfig.ASSESSMENT_SERVICE_BASE_URL);
    }

    protected E2eHttpClient gatewayClient() {
        return new E2eHttpClient(E2eConfig.BASE_URL);
    }

    protected E2eDatabase userDatabase() throws SQLException {
        return E2eDatabase.connect("user_db");
    }

    protected E2eDatabase learningDatabase() throws SQLException {
        return E2eDatabase.connect("learning_service_db");
    }

    protected E2eDatabase assessmentDatabase() throws SQLException {
        return E2eDatabase.connect("assessment_db");
    }

    protected Map<String, String> internalHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Internal-Api-Key", E2eConfig.INTERNAL_API_KEY);
        return headers;
    }

    protected Map<String, String> bearer(String token) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        return headers;
    }

    protected Map<String, Object> data(Object... values) {
        return objectData(values);
    }

    protected Map<String, Object> objectData(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    protected Object apiValue(E2eHttpClient.Response response) {
        return response.value();
    }

    protected Map<String, Object> apiData(E2eHttpClient.Response response) {
        return response.data();
    }

    protected int apiCode(E2eHttpClient.Response response) {
        return response.apiCode();
    }

    protected String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    protected Long longValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
    }

    protected Map<String, Object> merge(Object... values) {
        return objectData(values);
    }

    protected String stamp() {
        return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
    }

    protected void ensureUserServiceAdmin(E2eDatabase database) throws SQLException {
        String count = database.scalar("SELECT COUNT(*) FROM `user` WHERE role='admin' AND status=1");
        if (!"0".equals(count)) return;
        database.execute("INSERT INTO `user` (username,password,role,name,status) VALUES (?,?,?,?,1)",
                "e2e_admin", "$2a$10$EQsEXjoadhew/SGvon4uF.T2wNWI2XeyEqQgqxigBD4sdV6gUCHWq",
                "admin", "E2E 管理员");
    }

    protected Long userId(E2eDatabase database, String username) throws SQLException {
        return longValue(database.scalar("SELECT id FROM `user` WHERE username=?", username));
    }

    protected void cleanupUser(E2eDatabase database, String username) {
        if (database == null || username == null) return;
        try {
            database.execute("DELETE FROM `user` WHERE username=?", username);
        } catch (SQLException ignored) {
        }
    }

    protected void cleanupCourse(E2eDatabase database, Long courseId) {
        if (database == null || courseId == null) return;
        try {
            database.execute("DELETE FROM course WHERE id=?", courseId);
        } catch (SQLException ignored) {
        }
    }

    protected String json(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
