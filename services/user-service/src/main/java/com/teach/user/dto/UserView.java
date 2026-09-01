package com.teach.user.dto;

import com.teach.user.entity.User;

import java.sql.Timestamp;

/** 对外返回的用户视图，不携带密码。 */
public class UserView {
    private Long id;
    private String username;
    private String role;
    private String name;
    private String email;
    private String avatarUrl;
    private Integer status;
    private Timestamp createdAt;

    public static UserView from(User user) {
        if (user == null) return null;
        UserView v = new UserView();
        v.id = user.getId();
        v.username = user.getUsername();
        v.role = user.getRole();
        v.name = user.getName();
        v.email = user.getEmail();
        v.avatarUrl = user.getAvatarUrl();
        v.status = user.getStatus();
        v.createdAt = user.getCreatedAt();
        return v;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
