package org.example.e2e;

import java.util.LinkedHashMap;
import java.util.Map;

final class E2eConfig {
    private static final Map<String, String> DOT_ENV = loadDotEnv();
    static final String BASE_URL = env("E2E_BASE_URL", "http://localhost:3000");
    static final String USER_SERVICE_BASE_URL = env("E2E_USER_SERVICE_URL", "http://localhost:8082");
    static final String LEARNING_SERVICE_BASE_URL = env("E2E_LEARNING_SERVICE_URL", "http://localhost:8083");
    static final String ASSESSMENT_SERVICE_BASE_URL = env("E2E_ASSESSMENT_SERVICE_URL", "http://localhost:8084");
    static final String INTERNAL_API_KEY = env("E2E_INTERNAL_API_KEY",
            env("INTERNAL_API_KEY", "dev-internal-key"));
    static final String DB_HOST = env("E2E_DB_HOST", "127.0.0.1");
    static final String DB_PORT = env("E2E_DB_PORT", env("MICROSERVICES_MYSQL_PORT", "3307"));
    static final String DB_USERNAME = env("E2E_DB_USERNAME", "root");
    static final String DB_PASSWORD = env("E2E_DB_PASSWORD",
            env("MICROSERVICES_DB_ROOT_PASSWORD", "root123456"));
    static final String MATRIX_FILE = env("E2E_MATRIX_FILE", "ci-artifacts/java-e2e-matrix.json");
    static final String MATRIX_CSV_FILE = env("E2E_MATRIX_CSV_FILE", "ci-artifacts/java-e2e-matrix.csv");

    private E2eConfig() {
    }

    static String jdbcUrl(String schema) {
        return "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + schema
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8";
    }

    static Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseUrl", BASE_URL);
        result.put("userServiceUrl", USER_SERVICE_BASE_URL);
        result.put("learningServiceUrl", LEARNING_SERVICE_BASE_URL);
        result.put("assessmentServiceUrl", ASSESSMENT_SERVICE_BASE_URL);
        result.put("database", DB_HOST + ":" + DB_PORT);
        result.put("databaseUser", DB_USERNAME);
        return result;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) value = DOT_ENV.get(name);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> result = new LinkedHashMap<>();
        java.io.File file = new java.io.File(".env");
        if (!file.isFile()) return result;
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int separator = trimmed.indexOf('=');
                if (separator > 0) result.put(trimmed.substring(0, separator).trim(),
                        trimmed.substring(separator + 1).trim());
            }
        } catch (java.io.IOException ignored) {
        }
        return result;
    }
}
