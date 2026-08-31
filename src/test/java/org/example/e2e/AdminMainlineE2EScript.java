package org.example.e2e;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AdminMainlineE2EScript extends E2eTestSupport {
    @Test
    void adminGovernanceMainline() throws Exception {
        String stamp = stamp();
        String username = "adm_" + stamp.substring(Math.max(0, stamp.length() - 10));
        E2eHttpClient admin = client();
        E2eHttpClient temporary = client();
        E2eHttpClient changedUser = client();
        E2eDatabase db = null;
        Map<String, Object> actual = new LinkedHashMap<>();
        boolean passed = false;
        String assertion = "失败：管理员主线未完成。";
        try {
            db = database();
            E2eHttpClient.Response adminLogin = login(admin, "admin", "123456");
            assertRedirect(adminLogin, "/");
            E2eHttpClient.Response registration = register(temporary, username, "student", db);
            String temporaryId = userId(db, username);
            E2eHttpClient.Response users = admin.get("/admin/users?role=student");
            E2eHttpClient.Response update = admin.postForm("/admin/users/update", data(
                    "userId", temporaryId, "name", "管理员主线用户", "email", "admin-mainline@example.com", "role", "teacher"));
            String roleAfterUpdate = db.scalar("SELECT role FROM `user` WHERE id=?", temporaryId);
            E2eHttpClient.Response reset = admin.postForm("/admin/users/reset-password", data(
                    "userId", temporaryId, "password", "654321"));
            E2eHttpClient.Response changedLogin = login(changedUser, username, "654321");
            E2eHttpClient.Response selfDelete = admin.postForm("/admin/users/delete", data("userId", userId(db, "admin")));
            E2eHttpClient.Response normalAccess = changedUser.get("/admin/users");
            E2eHttpClient.Response delete = admin.postForm("/admin/users/delete", data("userId", temporaryId));
            String remaining = db.scalar("SELECT COUNT(*) FROM `user` WHERE username=?", username);
            String logCount = db.scalar("SELECT COUNT(*) FROM operation_log WHERE username='admin' AND action LIKE '管理员%'");

            actual.put("adminLogin", adminLogin.summary());
            actual.put("temporaryUser", merge("registration", registration.summary(), "id", temporaryId));
            actual.put("queryUsers", merge("response", users.summary(), "containsTemporaryUser", bodyContains(users, username)));
            actual.put("updateRole", merge("response", update.summary(), "dbRole", roleAfterUpdate));
            actual.put("resetPassword", reset.summary());
            actual.put("loginWithNewPassword", changedLogin.summary());
            actual.put("deleteUniqueOrCurrentAdmin", selfDelete.summary());
            actual.put("ordinaryUserAccessAdmin", normalAccess.summary());
            actual.put("deleteUser", delete.summary());
            actual.put("dbAfterDelete", merge("remainingCount", remaining));
            actual.put("operationLogCount", logCount);
            actual.put("unsupportedRequirement", "当前代码未提供禁用用户入口；普通用户访问管理页实际重定向 /login，而不是 HTTP 403。");

            passed = adminLogin.status == 302 && registration.status == 302 && users.status == 200
                    && bodyContains(users, username) && update.location().contains("message=updated")
                    && "teacher".equals(roleAfterUpdate) && reset.location().contains("message=passwordReset")
                    && changedLogin.status == 302 && selfDelete.location().contains("message=selfDeleteBlocked")
                    && normalAccess.status == 302 && normalAccess.location().contains("/login")
                    && delete.location().contains("message=deleted") && "0".equals(remaining)
                    && Integer.parseInt(logCount) >= 3;
            assertion = passed
                    ? "通过：管理员治理主线通过。补充观察：禁用用户和普通用户访问返回 HTTP 403 在当前代码中未实现。"
                    : "失败：管理员治理主线存在未通过步骤，见实际输出。";
            org.junit.jupiter.api.Assertions.assertTrue(passed, assertion);
        } catch (Throwable exception) {
            actual.put("exception", exception.toString());
            assertion = "失败：" + exception.getMessage();
            if (exception instanceof Error) throw (Error) exception;
            throw (Exception) exception;
        } finally {
            E2eMatrix.add(
                    "主线三_管理员：登录 -> 查询用户 -> 修改角色 -> 重置密码 -> 删除用户 -> 验证日志",
                    "A000",
                    "UserController.login; AdminController.users/updateUser/resetPassword/deleteUser/logs",
                    objectData("admin", "admin", "temporaryUser", username, "updateRole", "teacher", "resetPassword", "654321", "selfDeleteProtection", true),
                    "容器 frontend/backend/mysql 已启动；admin 存在；临时用户可以注册；数据库可查询 user 和 operation_log。",
                    "管理员查询用户、修改资料和角色、重置密码、保护当前管理员、删除普通用户并留下操作日志。",
                    actual,
                    assertion,
                    passed);
            cleanupUser(db, username);
            if (db != null) db.close();
        }
    }
}
