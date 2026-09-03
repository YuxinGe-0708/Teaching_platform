package com.teach.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teach.user.entity.User;
import com.teach.user.security.JwtUtil;
import com.teach.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerApiTest {
    private UserService users; private JwtUtil jwt; private MockMvc mvc;
    @BeforeEach void setup() { users=mock(UserService.class); jwt=mock(JwtUtil.class); mvc=MockMvcBuilders.standaloneSetup(new AuthController(users,jwt)).build(); }

    @Test void loginReturnsTokenAndUserContract() throws Exception {
        User user=new User(); user.setId(9L); user.setUsername("student9"); user.setRole("student"); user.setStatus(1);
        when(users.login("student9","123456")).thenReturn(user); when(jwt.createToken(9L,"student9","student")).thenReturn("jwt-token");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"student9\",\"password\":\"123456\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.token").value("jwt-token"));
    }

    @Test void registerValidatesRole() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"student9\",\"password\":\"123456\",\"role\":\"admin\"}"))
                .andExpect(status().isBadRequest());
    }
}
