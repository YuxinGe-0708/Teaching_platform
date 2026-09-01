package com.teach.user.controller;

import com.teach.user.entity.Notification;
import com.teach.user.entity.OperationLog;
import com.teach.user.entity.User;
import com.teach.user.security.IdentityContext;
import com.teach.user.security.UserIdentity;
import com.teach.user.service.NotificationService;
import com.teach.user.service.OperationLogService;
import com.teach.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** API contract tests for every public user-service endpoint except auth (covered separately). */
class PublicControllerApiTest {
    private UserService users;
    private NotificationService notifications;
    private OperationLogService logs;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        users = mock(UserService.class);
        notifications = mock(NotificationService.class);
        logs = mock(OperationLogService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new AdminController(users, logs),
                new ProfileController(users),
                new NotificationController(notifications),
                new VersionController()).build();
        IdentityContext.set(new UserIdentity(7L, "admin", "admin"));
    }

    @AfterEach void clearIdentity() { IdentityContext.clear(); }

    @Test
    void adminEndpointsCoverSuccessAndBusinessFailures() throws Exception {
        when(users.listUsers("student")).thenReturn(Collections.singletonList(user(8L, "student", "student")));
        when(logs.findRecent()).thenReturn(Collections.singletonList(new OperationLog()));
        when(users.updateStatus(8L, 0)).thenReturn(true);
        when(users.resetPassword(8L, "123456")).thenReturn(true);
        when(users.deleteUser(8L)).thenReturn(true);

        mvc.perform(get("/api/users").param("role", "student")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(put("/api/users/8/status").param("status", "0")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/api/users/8/reset-password").param("password", "123456")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(delete("/api/users/8")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/logs")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        when(users.updateStatus(99L, 0)).thenReturn(false);
        when(users.resetPassword(99L, "short")).thenReturn(false);
        when(users.deleteUser(99L)).thenReturn(false);
        mvc.perform(put("/api/users/99/status").param("status", "0")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(post("/api/users/99/reset-password").param("password", "short")).andExpect(jsonPath("$.code").value(400));
        mvc.perform(delete("/api/users/99")).andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void adminEndpointsRejectNonAdmin() throws Exception {
        IdentityContext.set(new UserIdentity(7L, "student", "student"));
        assertThrows(Exception.class, () -> mvc.perform(get("/api/users")));
    }

    @Test
    void profileEndpointsCoverFoundUpdateAndMissingUser() throws Exception {
        User current = user(7L, "admin", "admin");
        when(users.findById(7L)).thenReturn(current);
        mvc.perform(get("/api/profile")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(7));
        mvc.perform(put("/api/profile").param("name", " New Name ").param("email", "a@b.test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(users).updateProfile(argThat(u -> "New Name".equals(u.getName()) && "a@b.test".equals(u.getEmail())));

        when(users.findById(7L)).thenReturn(null);
        mvc.perform(get("/api/profile")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(put("/api/profile").param("name", "x")).andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void notificationAndVersionEndpointsAreCovered() throws Exception {
        Notification n = new Notification(); n.setId(12L); n.setUserId(7L);
        when(notifications.findByUserId(7L)).thenReturn(Collections.singletonList(n));
        when(notifications.countUnread(7L)).thenReturn(1);
        mvc.perform(get("/api/notifications")).andExpect(jsonPath("$.data[0].id").value(12));
        mvc.perform(get("/api/notifications/unread-count")).andExpect(jsonPath("$.data").value(1));
        mvc.perform(post("/api/notifications/read").param("notificationId", "12")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/api/notifications/read-all")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/version")).andExpect(jsonPath("$.code").value(200));
        verify(notifications).markAsRead(12L, 7L);
        verify(notifications).markAllAsRead(7L);
    }

    private User user(Long id, String username, String role) {
        User u = new User(); u.setId(id); u.setUsername(username); u.setName(username); u.setRole(role); u.setStatus(1); return u;
    }
}
