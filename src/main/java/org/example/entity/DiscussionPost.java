package org.example.entity;

public class DiscussionPost {
    private Long id;
    private Long courseId;
    private Long userId;
    private String title;
    private String content;
    private java.sql.Timestamp createdAt;
    private String authorName;
    private String authorRole;
    private Boolean anonymous;
    private String postType;
    private String targetRole;
    private Long targetUserId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public java.sql.Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
    public Boolean getAnonymous() { return anonymous; }
    public void setAnonymous(Boolean anonymous) { this.anonymous = anonymous; }
    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public String getDisplayAuthorName() {
        return Boolean.TRUE.equals(anonymous) ? "匿名同学" : authorName;
    }

    public String getPostTypeLabel() {
        if ("question".equals(postType)) return "提问";
        if ("share".equals(postType)) return "心得";
        if ("resource".equals(postType)) return "资源";
        return "讨论";
    }

    public String getTargetRoleLabel() {
        if ("teacher".equals(targetRole)) return "推送给教师";
        if ("assistant".equals(targetRole)) return "推送给助教";
        return "全班可见";
    }
}
