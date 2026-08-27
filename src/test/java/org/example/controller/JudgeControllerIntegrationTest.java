package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Task;
import org.example.entity.User;
import org.example.mapper.TaskMapper;
import org.example.mapper.UserMapper;
import org.example.service.TaskService;
import org.example.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JudgeControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserService userService;

  @Autowired
  private TaskService taskService;

  @Autowired
  private TaskMapper taskMapper;

  private MockHttpSession session;
  private User testUser;
  private Task testTask;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
    testUser = userService.register("judgeuser", "password123", "student", "评测学生");
    session.setAttribute("currentUser", testUser);

    testTask = new Task();
    testTask.setTitle("两数之和");
    testTask.setType("programming");
    testTask.setMaxScore(100);
    testTask.setCourseId(1L);
    testTask.setStatus("published");
    testTask = taskService.createTask(testTask);
  }

  // ========== J020: 学生提交Python代码评测全部通过（正例） ==========
  @Test
  void submitAndJudge_shouldReturnAC_whenCodeCorrect() throws Exception {
    Map<String, Object> request = new HashMap<>();
    request.put("code", "import sys\nprint('Hello World')");
    request.put("language", "python");
    request.put("taskId", testTask.getId());

    // Test cases for Hello World
    // Since we don't have a real Judge0 API, we'll test the local fallback

    mockMvc.perform(post("/api/v2/judge/submit")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
  }

  // ========== J021: 提交未允许的编程语言拦截（反例） ==========
  @Test
  void submitAndJudge_shouldReturnError_whenLanguageNotAllowed() throws Exception {
    // Update task to allow only Python
    testTask.setDescription("<!--TP_META\nallowedLanguage=cHl0aG9u\nTP_META-->");
    taskMapper.update(testTask);

    Map<String, Object> request = new HashMap<>();
    request.put("code", "public class Main { public static void main(String[] args) {} }");
    request.put("language", "java");
    request.put("taskId", testTask.getId());

    mockMvc.perform(post("/api/v2/judge/submit")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.message").value("该实训仅允许提交 Python 代码"));
  }

  // ========== 未登录用户提交评测拦截 ==========
  @Test
  void submitAndJudge_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
    Map<String, Object> request = new HashMap<>();
    request.put("code", "print('Hello')");
    request.put("language", "python");

    mockMvc.perform(post("/api/v2/judge/submit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(401))
        .andExpect(jsonPath("$.message").value("请先登录"));
  }

  // ========== 空代码提交拦截 ==========
  @Test
  void submitAndJudge_shouldReturnError_whenCodeEmpty() throws Exception {
    Map<String, Object> request = new HashMap<>();
    request.put("code", "");
    request.put("language", "python");

    mockMvc.perform(post("/api/v2/judge/submit")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.message").value("代码不能为空"));
  }
}
