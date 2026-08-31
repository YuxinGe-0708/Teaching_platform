package com.teach.learning.entity;
import java.sql.Timestamp;
public class StudyNote {
    private Long id; private Long studentId; private Long courseId; private Long resourceId;
    private String title; private String content; private String aiSummary; private String mindMap;
    private Timestamp createdAt; private Timestamp updatedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; } public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCourseId() { return courseId; } public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getResourceId() { return resourceId; } public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; } public void setContent(String content) { this.content = content; }
    public String getAiSummary() { return aiSummary; } public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public String getMindMap() { return mindMap; } public void setMindMap(String mindMap) { this.mindMap = mindMap; }
    public Timestamp getCreatedAt() { return createdAt; } public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
