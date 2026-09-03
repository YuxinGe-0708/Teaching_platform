package org.example.controller;

import org.example.bff.MicroserviceClient;
import org.example.dto.ApiResponse;
import org.example.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BffDelegationTest {
    @Test
    void judgeRequiresAnAuthenticatedSession() {
        MicroserviceClient client = mock(MicroserviceClient.class);
        ApiResponse<Map<String, Object>> response =
                new JudgeController(client).submitAndJudge(Collections.emptyMap(), new MockHttpSession());

        assertEquals(401, response.getCode());
        verify(client, never()).post(any(), any(), eq(Map.class));
    }

    @Test
    void judgeUsesTheLoggedInStudentInsteadOfClientSuppliedIdentity() {
        MicroserviceClient client = mock(MicroserviceClient.class);
        when(client.assessment("/api/v2/judge/submit")).thenReturn("http://assessment/api/v2/judge/submit");
        when(client.post(eq("http://assessment/api/v2/judge/submit"), any(), eq(Map.class)))
                .thenReturn(Collections.singletonMap("status", "AC"));
        MockHttpSession session = studentSession(42L);
        Map<String, Object> request = new HashMap<>();
        request.put("studentId", 999L);
        request.put("code", "print(1)");

        ApiResponse<Map<String, Object>> response =
                new JudgeController(client).submitAndJudge(request, session);

        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(client).post(eq("http://assessment/api/v2/judge/submit"), body.capture(), eq(Map.class));
        assertEquals(42L, ((Map<?, ?>) body.getValue()).get("studentId"));
        assertEquals("AC", response.getData().get("status"));
    }

    @Test
    void aiConversationIsScopedToTheLoggedInUser() {
        MicroserviceClient client = mock(MicroserviceClient.class);
        when(client.learning("/api/v2/ai/chat")).thenReturn("http://learning/api/v2/ai/chat");
        when(client.post(eq("http://learning/api/v2/ai/chat"), any(), eq(Map.class)))
                .thenReturn(Collections.singletonMap("reply", "ok"));
        Map<String, String> request = new HashMap<>();
        request.put("message", "hello");
        request.put("courseId", "7");
        request.put("sessionId", "forged");

        ApiResponse<Map<String, String>> response =
                new AiController(client).chat(request, studentSession(42L));

        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(client).post(eq("http://learning/api/v2/ai/chat"), body.capture(), eq(Map.class));
        assertEquals("user_42_course_7", ((Map<?, ?>) body.getValue()).get("sessionId"));
        assertFalse(response.getData().get("reply").isEmpty());
    }

    private MockHttpSession studentSession(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole("student");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", user);
        return session;
    }
}
