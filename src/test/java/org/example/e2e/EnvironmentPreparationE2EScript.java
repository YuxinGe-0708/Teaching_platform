package org.example.e2e;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnvironmentPreparationE2EScript extends E2eTestSupport {
    @Test
    void verifyDeployedEnvironmentAndSeedData() throws Exception {
        Map<String, Object> actual = new LinkedHashMap<>();
        boolean passed = false;
        String assertion = "失败：E2E 环境不可用。";
        E2eDatabase db = null;
        Path generatedPdf = null;
        Path generatedVideo = null;
        try {
            E2eHttpClient client = client();
            E2eHttpClient.Response login = client.get("/login");
            db = database();
            if (!Files.exists(E2eConfig.PDF_PATH)) generatedPdf = createPdfFixture();
            if (!Files.exists(E2eConfig.VIDEO_PATH)) generatedVideo = createVideoFixture();
            Map<String, String> accounts = new LinkedHashMap<>();
            for (String username : new String[] {"admin", "teacher_demo", "student_006", "student_005"}) {
                accounts.put(username, db.scalar("SELECT COUNT(*) FROM `user` WHERE username=?", username));
            }
            actual.put("config", E2eConfig.summary());
            actual.put("server", login.summary());
            actual.put("seedAccounts", accounts);
            actual.put("files", objectData("pdfPath", generatedPdf == null ? E2eConfig.PDF_PATH.toString() : generatedPdf.toString(), "pdfExists", Files.exists(generatedPdf == null ? E2eConfig.PDF_PATH : generatedPdf),
                    "videoPath", generatedVideo == null ? E2eConfig.VIDEO_PATH.toString() : generatedVideo.toString(), "videoExists", Files.exists(generatedVideo == null ? E2eConfig.VIDEO_PATH : generatedVideo)));
            passed = login.status == 200 && "1".equals(db.scalar("SELECT 1"))
                    && accounts.values().stream().allMatch("1"::equals)
                    && Files.exists(generatedPdf == null ? E2eConfig.PDF_PATH : generatedPdf) && Files.exists(generatedVideo == null ? E2eConfig.VIDEO_PATH : generatedVideo);
            assertion = passed ? "通过：端到端测试环境可用。" : "失败/阻塞：端到端测试环境不可用，需先启动容器或补齐数据库/文件。";
            org.junit.jupiter.api.Assertions.assertTrue(passed, assertion);
        } catch (Throwable exception) {
            actual.put("exception", exception.toString());
            assertion = "失败/阻塞：" + exception.getMessage();
            if (exception instanceof Error) throw (Error) exception;
            throw (Exception) exception;
        } finally {
            E2eMatrix.add(
                    "准备工作：部署所有服务 + 准备测试数据",
                    "E000",
                    "GET /login; JDBC SELECT 1; JDBC SELECT COUNT(*) FROM user WHERE username IN (...)",
                    objectData("baseUrl", E2eConfig.BASE_URL, "db", E2eConfig.summary(),
                            "requiredAccounts", new String[] {"admin", "teacher_demo", "student_006", "student_005"},
                            "requiredFiles", new String[] {E2eConfig.PDF_PATH.toString(), E2eConfig.VIDEO_PATH.toString()}),
                    "frontend、backend、mysql 容器已启动；测试数据已通过 APP_SEED_TEST_DATA 或初始化脚本准备。",
                    "登录页返回 200；数据库连接正常；四个测试账号和上传样例文件存在。",
                    actual,
                    assertion,
                    passed);
            if (db != null) db.close();
            deleteTempFixture(generatedPdf);
            deleteTempFixture(generatedVideo);
        }
    }
}
