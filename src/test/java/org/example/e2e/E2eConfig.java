package org.example.e2e;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

final class E2eConfig {
    static final Path ROOT = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    private static final Map<String, String> DOT_ENV = loadDotEnv(ROOT.resolve(".env"));
    static final String BASE_URL = value("E2E_BASE_URL", "BASE_URL", "http://localhost:3000");
    static final String DB_URL = databaseUrl();
    static final String DB_USER = value("E2E_DB_USER", "DB_USERNAME", value("MYSQL_USER", "tp_dev"));
    static final String DB_PASSWORD = value("E2E_DB_PASSWORD", "DB_PASSWORD", value("MYSQL_PASSWORD", "123456"));
    static final String DB_NAME = value("E2E_DB_NAME", "DB_NAME", value("MYSQL_DATABASE", "teaching_platform"));
    static final Path PDF_PATH = pathValue("E2E_PDF_PATH", ROOT.resolve("docs").resolve("测试文档_样例.pdf"));
    static final Path VIDEO_PATH = pathValue("E2E_VIDEO_PATH", ROOT.resolve("uploads").resolve("resources").resolve("2").resolve("1780801920737_bandicam 2026-06-07 10-15-00-438.mp4"));

    private E2eConfig() {
    }

    static Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseUrl", BASE_URL);
        result.put("dbUrl", DB_URL.replaceFirst("(?i)(password=)[^&]*", "$1***"));
        result.put("dbUser", DB_USER);
        result.put("dbName", DB_NAME);
        result.put("dbTransport", "JDBC to configured DB_URL; local Compose .env uses localhost:3307, CI Compose defaults to localhost:3306");
        result.put("pdfPath", PDF_PATH.toString());
        result.put("videoPath", VIDEO_PATH.toString());
        return result;
    }

    static Path pathValue(String envName, Path defaultPath) {
        String value = value(envName, null, null);
        return value == null || value.trim().isEmpty() ? defaultPath : Paths.get(value).toAbsolutePath().normalize();
    }

    static String value(String primary, String secondary, String defaultValue) {
        String value = nonBlank(System.getProperty(primary));
        if (value != null) return value;
        value = nonBlank(System.getenv(primary));
        if (value != null) return value;
        value = nonBlank(DOT_ENV.get(primary));
        if (value != null) return value;
        if (secondary != null) {
            value = nonBlank(System.getProperty(secondary));
            if (value != null) return value;
            value = nonBlank(System.getenv(secondary));
            if (value != null) return value;
            value = nonBlank(DOT_ENV.get(secondary));
            if (value != null) return value;
        }
        return defaultValue;
    }

    static String value(String primary, String defaultValue) {
        return value(primary, null, defaultValue);
    }

    private static String databaseUrl() {
        String explicit = value("E2E_DB_URL", "DB_URL", null);
        if (explicit != null) return explicit;
        String host = value("E2E_DB_HOST", "DB_HOST", "127.0.0.1");
        String port = value("E2E_DB_PORT", "DB_PORT", value("MYSQL_PORT", "3306"));
        String database = value("E2E_DB_NAME", "DB_NAME", value("MYSQL_DATABASE", "teaching_platform"));
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8";
    }

    private static String nonBlank(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static Map<String, String> loadDotEnv(Path path) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!Files.exists(path)) return result;
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
                int separator = line.indexOf('=');
                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                result.put(key, value);
            }
        } catch (IOException ignored) {
            // The environment variables remain the authoritative override.
        }
        return result;
    }
}
