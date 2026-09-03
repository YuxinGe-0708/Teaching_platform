package com.teach.learning.entity;
import java.sql.Timestamp;
public class DiscussionPost {
    private Long id; private Long courseId; private Long userId; private String title;
    private String content; private Boolean anonymous; private String postType;
    private String targetRole; private Long targetUserId; private Timestamp createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getCourseId() { return courseId; } public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; } public void setContent(String content) { this.content = content; }
    public Boolean getAnonymous() { return anonymous; } public void setAnonymous(Boolean anonymous) { this.anonymous = anonymous; }
    public String getPostType() { return postType; } public void setPostType(String postType) { this.postType = postType; }
    public String getTargetRole() { return targetRole; } public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public Long getTargetUserId() { return targetUserId; } public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public Timestamp getCreatedAt() { return createdAt; } public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
