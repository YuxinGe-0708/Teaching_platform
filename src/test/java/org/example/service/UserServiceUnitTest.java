package org.example.service;

import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserService userService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("student01");
    testUser.setPassword("encodedPassword");
    testUser.setRole("student");
    testUser.setName("张同学");
  }

  // ========== UC01: 用户注册成功（正例） ==========
  @Test
  void register_shouldReturnUser_whenUsernameNotExists() {
    // Given
    String username = "student01";
    String password = "123456";
    String role = "student";
    String name = "张同学";

    when(userMapper.findByUsername(username)).thenReturn(null);
    when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
    when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setId(1L);
      return 1;
    });

    // When
    User result = userService.register(username, password, role, name);

    // Then
    assertNotNull(result);
    assertEquals("student01", result.getUsername());
    assertEquals("student", result.getRole());
    assertEquals("张同学", result.getName());
    verify(userMapper).insert(any(User.class));
  }

  // ========== UC011: 用户注册重名拦截（反例） ==========
  @Test
  void register_shouldReturnNull_whenUsernameAlreadyExists() {
    // Given
    String username = "student01";
    when(userMapper.findByUsername(username)).thenReturn(testUser);

    // When
    User result = userService.register(username, "123456", "student", "张同学");

    // Then
    assertNull(result);
    verify(userMapper, never()).insert(any(User.class));
  }

  // ========== UC012: 用户登录密码错误拦截（反例） ==========
  @Test
  void login_shouldReturnNull_whenPasswordWrong() {
    // Given
    String username = "student01";
    String wrongPassword = "wrongpass";
    when(userMapper.findByUsername(username)).thenReturn(testUser);
    when(passwordEncoder.matches(wrongPassword, testUser.getPassword())).thenReturn(false);

    // When
    User result = userService.login(username, wrongPassword);

    // Then
    assertNull(result);
  }

  // ========== UC030: 管理员修改用户资料（正例） ==========
  @Test
  void updateByAdmin_shouldReturnTrue_whenUserExists() {
    // Given
    Long userId = 2L;
    String name = "李老师";
    String email = "li@test.com";
    String role = "teacher";

    User existingUser = new User();
    existingUser.setId(userId);
    existingUser.setUsername("teacher01");
    existingUser.setName("旧名字");
    existingUser.setEmail("old@test.com");
    existingUser.setRole("student");

    when(userMapper.findById(userId)).thenReturn(existingUser);
    when(userMapper.updateByAdmin(any(User.class))).thenReturn(1);

    // When
    boolean result = userService.updateByAdmin(userId, name, email, role);

    // Then
    assertTrue(result);
    assertEquals("李老师", existingUser.getName());
    assertEquals("li@test.com", existingUser.getEmail());
    assertEquals("teacher", existingUser.getRole());
    verify(userMapper).updateByAdmin(existingUser);
  }

  // ========== UC031: 管理员重置密码格式过短拦截（反例） ==========
  @Test
  void resetPassword_shouldReturnFalse_whenPasswordTooShort() {
    // Given
    Long userId = 2L;
    String shortPassword = "123";

    // When
    boolean result = userService.resetPassword(userId, shortPassword);

    // Then
    assertFalse(result);
    verify(userMapper, never()).updatePassword(anyLong(), anyString());
  }

  // ========== 管理员自删除保护（反例） ==========
  @Test
  void deleteUser_shouldReturnTrue_whenUserExistsAndNotSelf() {
    // Given
    Long userId = 2L;
    when(userMapper.deleteById(userId)).thenReturn(1);

    // When
    boolean result = userService.deleteUser(userId);

    // Then
    assertTrue(result);
    verify(userMapper).deleteById(userId);
  }

  // ========== listUsers 角色过滤 ==========
  @Test
  void listUsers_shouldReturnAllUsers_whenRoleNull() {
    // Given
    List<User> users = Arrays.asList(testUser, new User());
    when(userMapper.findAll()).thenReturn(users);

    // When
    List<User> result = userService.listUsers(null);

    // Then
    assertEquals(2, result.size());
    verify(userMapper).findAll();
  }

  @Test
  void listUsers_shouldReturnFilteredUsers_whenRoleValid() {
    // Given
    List<User> students = Arrays.asList(testUser);
    when(userMapper.findByRole("student")).thenReturn(students);

    // When
    List<User> result = userService.listUsers("student");

    // Then
    assertEquals(1, result.size());
    assertEquals("student", result.get(0).getRole());
    verify(userMapper).findByRole("student");
  }
}
