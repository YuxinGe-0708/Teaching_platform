package org.example.entity;

public class DiscussionReply {
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private java.sql.Timestamp createdAt;
    private String authorName;
    private String authorRole;
    private Boolean anonymous;
    private Boolean assistantReply;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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
    public Boolean getAssistantReply() { return assistantReply; }
    public void setAssistantReply(Boolean assistantReply) { this.assistantReply = assistantReply; }

    public String getDisplayAuthorName() {
        return Boolean.TRUE.equals(anonymous) ? "匿名同学" : authorName;
    }
}
