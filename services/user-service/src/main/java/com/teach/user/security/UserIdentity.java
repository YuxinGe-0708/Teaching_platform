package com.teach.user.security;

/** 当前请求的已认证用户身份。 */
public class UserIdentity {
    private final Long userId;
    private final String username;
    private final String role;

    public UserIdentity(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
}
