package com.teach.user.service;

import com.teach.user.entity.User;
import com.teach.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setPassword("encoded_pwd");
        sampleUser.setRole("student");
        sampleUser.setName("测试用户");
        sampleUser.setEmail("test@teach.com");
        sampleUser.setStatus(1);
    }

    // ==================== 注册测试 ====================

    @Test
    @DisplayName("UNIT-TC-USER-01: 用户注册成功（密码加密且写入数据库）")
    void testRegister_Success() {
        when(userMapper.findByUsername("newuser")).thenReturn(null);
        when(passwordEncoder.encode("raw_password")).thenReturn("encoded_password");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        User result = userService.register("newuser", "raw_password", "student", "小明");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("encoded_password", result.getPassword());
        assertEquals("student", result.getRole());
        assertEquals("小明", result.getName());

        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    @DisplayName("UNIT-TC-USER-02: 用户注册失败（用户名已存在返回 null）")
    void testRegister_UsernameDuplicate_ReturnsNull() {
        when(userMapper.findByUsername("existing")).thenReturn(sampleUser);

        User result = userService.register("existing", "123456", "student", "小明");

        assertNull(result);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("UNIT-TC-USER-03: 用户注册（未填姓名时默认使用用户名）")
    void testRegister_NullName_UsesUsername() {
        when(userMapper.findByUsername("noname")).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("encoded_pwd");

        User result = userService.register("noname", "123456", "teacher", null);

        assertNotNull(result);
        assertEquals("noname", result.getName());
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("UNIT-TC-USER-04: 登录成功（正常用户且密码匹配）")
    void testLogin_Success() {
        when(userMapper.findByUsername("testuser")).thenReturn(sampleUser);
        when(passwordEncoder.matches("raw_pwd", "encoded_pwd")).thenReturn(true);

        User result = userService.login("testuser", "raw_pwd");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("UNIT-TC-USER-05: 登录失败（用户不存在）")
    void testLogin_UserNotFound_ReturnsNull() {
        when(userMapper.findByUsername("unknown")).thenReturn(null);

        User result = userService.login("unknown", "123456");

        assertNull(result);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("UNIT-TC-USER-06: 登录失败（用户已被禁用 status=0）")
    void testLogin_UserDisabled_ReturnsNull() {
        sampleUser.setStatus(0); // 禁用
        when(userMapper.findByUsername("disabled_user")).thenReturn(sampleUser);

        User result = userService.login("disabled_user", "raw_pwd");

        assertNull(result);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("UNIT-TC-USER-07: 登录失败（密码错误）")
    void testLogin_WrongPassword_ReturnsNull() {
        when(userMapper.findByUsername("testuser")).thenReturn(sampleUser);
        when(passwordEncoder.matches("wrong_pwd", "encoded_pwd")).thenReturn(false);

        User result = userService.login("testuser", "wrong_pwd");

        assertNull(result);
    }

    // ==================== 查询与个人信息测试 ====================

    @Test
    @DisplayName("UNIT-TC-USER-08: 按 ID 查询用户")
    void testFindById() {
        when(userMapper.findById(1L)).thenReturn(sampleUser);

        User result = userService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userMapper).findById(1L);
    }

    @Test
    @DisplayName("UNIT-TC-USER-09: 更新个人资料成功")
    void testUpdateProfile() {
        when(userMapper.updateProfile(sampleUser)).thenReturn(1);
        when(userMapper.findById(1L)).thenReturn(sampleUser);

        User result = userService.updateProfile(sampleUser);

        assertNotNull(result);
        verify(userMapper).updateProfile(sampleUser);
        verify(userMapper).findById(1L);
    }

    @Test
    @DisplayName("UNIT-TC-USER-10: 查询用户列表（角色有效）")
    void testListUsers_ValidRole() {
        when(userMapper.findByRole("student")).thenReturn(Collections.singletonList(sampleUser));

        List<User> list = userService.listUsers("student");

        assertEquals(1, list.size());
        verify(userMapper).findByRole("student");
        verify(userMapper, never()).findAll();
    }

    @Test
    @DisplayName("UNIT-TC-USER-11: 查询用户列表（角色为空或非法时查询全部）")
    void testListUsers_InvalidOrEmptyRole_FindAll() {
        when(userMapper.findAll()).thenReturn(Arrays.asList(sampleUser, new User()));

        List<User> listEmpty = userService.listUsers("   ");
        List<User> listInvalid = userService.listUsers("superman");

        assertEquals(2, listEmpty.size());
        assertEquals(2, listInvalid.size());
        verify(userMapper, times(2)).findAll();
    }

    // ==================== 管理员操作与密码重置测试 ====================

    @Test
    @DisplayName("UNIT-TC-USER-12: 管理员更新用户信息成功")
    void testUpdateByAdmin_Success() {
        when(userMapper.findById(1L)).thenReturn(sampleUser);
        when(userMapper.updateByAdmin(any(User.class))).thenReturn(1);

        boolean ok = userService.updateByAdmin(1L, "新名字", "new@qq.com", "teacher");

        assertTrue(ok);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateByAdmin(captor.capture());
        assertEquals("新名字", captor.getValue().getName());
        assertEquals("teacher", captor.getValue().getRole());
    }

    @Test
    @DisplayName("UNIT-TC-USER-13: 管理员更新失败（用户不存在或角色非法）")
    void testUpdateByAdmin_Fail() {
        // 情况 1: 用户不存在
        when(userMapper.findById(99L)).thenReturn(null);
        assertFalse(userService.updateByAdmin(99L, "名字", "email", "teacher"));

        // 情况 2: 角色非法
        when(userMapper.findById(1L)).thenReturn(sampleUser);
        assertFalse(userService.updateByAdmin(1L, "名字", "email", "invalid_role"));
    }

    @Test
    @DisplayName("UNIT-TC-USER-14: 更新用户状态")
    void testUpdateStatus() {
        when(userMapper.updateStatus(1L, 0)).thenReturn(1);
        when(userMapper.updateStatus(1L, 1)).thenReturn(1);

        assertTrue(userService.updateStatus(1L, 0));
        assertTrue(userService.updateStatus(1L, null)); // status 为 null 时默认为 1
    }

    @Test
    @DisplayName("UNIT-TC-USER-15: 重置密码（长度合规成功加密）")
    void testResetPassword_Success() {
        when(passwordEncoder.encode("newpassword123")).thenReturn("encoded_new");
        when(userMapper.updatePassword(1L, "encoded_new")).thenReturn(1);

        boolean ok = userService.resetPassword(1L, "newpassword123");

        assertTrue(ok);
        verify(userMapper).updatePassword(1L, "encoded_new");
    }

    @Test
    @DisplayName("UNIT-TC-USER-16: 重置密码失败（密码过短 <6 或过长 >32 或为 null）")
    void testResetPassword_InvalidLength_Fail() {
        assertFalse(userService.resetPassword(1L, null));
        assertFalse(userService.resetPassword(1L, "12345")); // 5位
        assertFalse(userService.resetPassword(1L, "123456789012345678901234567890123")); // 33位

        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    @DisplayName("UNIT-TC-USER-17: 删除用户")
    void testDeleteUser() {
        when(userMapper.deleteById(1L)).thenReturn(1);
        when(userMapper.deleteById(2L)).thenReturn(0);

        assertTrue(userService.deleteUser(1L));
        assertFalse(userService.deleteUser(2L));
    }
}