package com.teach.learning.dto;
import com.teach.learning.entity.DiscussionPost;
import java.sql.Timestamp;
public class DiscussionPostView {
    private Long id; private Long courseId; private Long userId; private String title;
    private String content; private Boolean anonymous; private String postType;
    private String targetRole; private Long targetUserId; private Timestamp createdAt;
    public static DiscussionPostView from(DiscussionPost p) {
        DiscussionPostView v = new DiscussionPostView(); v.id = p.getId(); v.courseId = p.getCourseId();
        v.userId = p.getUserId(); v.title = p.getTitle(); v.content = p.getContent();
        v.anonymous = p.getAnonymous(); v.postType = p.getPostType(); v.targetRole = p.getTargetRole();
        v.targetUserId = p.getTargetUserId(); v.createdAt = p.getCreatedAt(); return v;
    }
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
