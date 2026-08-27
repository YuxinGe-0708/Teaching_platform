package org.example.controller;

import org.example.entity.User;
import org.example.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserService userService;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  // ========== U020: 学生/教师登录并跳转主页成功（正例） ==========
  @Test
  void login_shouldRedirectToHome_whenValidCredentials() throws Exception {
    // First, register a test user
    userService.register("testteacher", "password123", "teacher", "测试老师");

    // Then login
    mockMvc.perform(post("/login")
            .param("username", "testteacher")
            .param("password", "password123")
            .session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));

    // Verify session has user
    User user = (User) session.getAttribute("currentUser");
    assertNotNull(user);
    assertEquals("testteacher", user.getUsername());
  }

  // ========== U021: 注册密码长度不合法拦截（反例） ==========
  @Test
  void register_shouldReturnError_whenPasswordTooShort() throws Exception {
    mockMvc.perform(post("/register")
            .param("username", "stu01")
            .param("password", "123")
            .param("role", "student"))
        .andExpect(status().isOk())
        .andExpect(view().name("register"))
        .andExpect(model().attributeExists("error"));
  }

  @Test
  void register_shouldReturnError_whenUsernameTooShort() throws Exception {
    mockMvc.perform(post("/register")
            .param("username", "ab")
            .param("password", "123456")
            .param("role", "student"))
        .andExpect(status().isOk())
        .andExpect(view().name("register"))
        .andExpect(model().attributeExists("error"));
  }

  // ========== 登录失败测试 ==========
  @Test
  void login_shouldReturnLoginPage_whenInvalidCredentials() throws Exception {
    mockMvc.perform(post("/login")
            .param("username", "nonexistent")
            .param("password", "wrong"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(model().attributeExists("error"));
  }

  // ========== 登出测试（修复版） ==========
  @Test
  void logout_shouldInvalidateSession() throws Exception {
    // Register and login
    userService.register("testuser", "password123", "student", "测试学生");

    mockMvc.perform(post("/login")
            .param("username", "testuser")
            .param("password", "password123")
            .session(session))
        .andExpect(status().is3xxRedirection());

    // Verify user is in session before logout
    User userBeforeLogout = (User) session.getAttribute("currentUser");
    assertNotNull(userBeforeLogout);

    // Perform logout
    mockMvc.perform(get("/logout").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));

    // ✅ 验证 session 已被 invalidate（访问会抛异常）
    assertThrows(IllegalStateException.class, () -> {
      session.getAttribute("currentUser");
    });
  }

  // ========== 注册成功后跳转登录页 ==========
  @Test
  void register_shouldRedirectToLogin_whenValid() throws Exception {
    mockMvc.perform(post("/register")
            .param("username", "newstudent")
            .param("password", "123456")
            .param("role", "student"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?registered=1"));
  }

  // ========== 注册重名拦截 ==========
  @Test
  void register_shouldReturnError_whenUsernameExists() throws Exception {
    // Register first user
    userService.register("existinguser", "password123", "student", "已存在用户");

    // Try to register same username
    mockMvc.perform(post("/register")
            .param("username", "existinguser")
            .param("password", "123456")
            .param("role", "student"))
        .andExpect(status().isOk())
        .andExpect(view().name("register"))
        .andExpect(model().attribute("error", "用户名已存在"));
  }

  // ========== 个人资料更新测试 ==========
  @Test
  void updateProfile_shouldUpdateUserInfo() throws Exception {
    // Login
    userService.register("profileuser", "password123", "student", "原名");
    mockMvc.perform(post("/login")
        .param("username", "profileuser")
        .param("password", "password123")
        .session(session));

    // Update profile
    mockMvc.perform(post("/profile")
            .param("name", "新名字")
            .param("email", "new@test.com")
            .session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/profile?updated=1"));

    // Verify update
    User user = (User) session.getAttribute("currentUser");
    assertNotNull(user);
    assertEquals("新名字", user.getName());
    assertEquals("new@test.com", user.getEmail());
  }
}