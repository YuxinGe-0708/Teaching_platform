package com.teach.user.service;

import com.teach.user.entity.User;
import com.teach.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 注册 student/teacher；用户名重复返回 null。 */
    public User register(String username, String password, String role, String name) {
        User existing = userMapper.findByUsername(username);
        if (existing != null) return null;

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setName(name != null ? name : username);
        userMapper.insert(user);
        return user;
    }

    /** 登录；账号不存在、密码错误或已禁用均返回 null。 */
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) return null;
        if (user.isDisabled()) return null;
        if (!passwordEncoder.matches(password, user.getPassword())) return null;
        return user;
    }

    public User findById(Long id) {
        return userMapper.findById(id);
    }

    public User updateProfile(User user) {
        userMapper.updateProfile(user);
        return userMapper.findById(user.getId());
    }

    public List<User> listUsers(String role) {
        if (role == null || role.trim().isEmpty()) return userMapper.findAll();
        String normalized = role.trim().toLowerCase();
        if (!isValidRole(normalized)) return userMapper.findAll();
        return userMapper.findByRole(normalized);
    }

    public boolean updateByAdmin(Long id, String name, String email, String role) {
        User user = userMapper.findById(id);
        String normalizedRole = role == null ? "" : role.trim().toLowerCase();
        if (user == null || !isValidRole(normalizedRole)) return false;
        user.setName(name == null || name.trim().isEmpty() ? user.getUsername() : name.trim());
        user.setEmail(email == null ? "" : email.trim());
        user.setRole(normalizedRole);
        return userMapper.updateByAdmin(user) > 0;
    }

    public boolean updateStatus(Long id, Integer status) {
        return userMapper.updateStatus(id, status == null ? 1 : status) > 0;
    }

    public boolean resetPassword(Long id, String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6 || rawPassword.length() > 32) return false;
        return userMapper.updatePassword(id, passwordEncoder.encode(rawPassword.trim())) > 0;
    }

    public boolean deleteUser(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    private boolean isValidRole(String role) {
        return "student".equals(role) || "teacher".equals(role) || "admin".equals(role);
    }
}
