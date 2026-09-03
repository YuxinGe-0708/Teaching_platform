package com.teach.learning.entity;
import java.sql.Timestamp;
public class DiscussionReply {
    private Long id; private Long postId; private Long userId; private String content;
    private Boolean anonymous; private Boolean assistantReply; private Timestamp createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; } public void setPostId(Long postId) { this.postId = postId; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; } public void setContent(String content) { this.content = content; }
    public Boolean getAnonymous() { return anonymous; } public void setAnonymous(Boolean anonymous) { this.anonymous = anonymous; }
    public Boolean getAssistantReply() { return assistantReply; } public void setAssistantReply(Boolean assistantReply) { this.assistantReply = assistantReply; }
    public Timestamp getCreatedAt() { return createdAt; } public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
