package org.example.entity;

public class StudyNote {
    private Long id;
    private Long studentId;
    private Long courseId;
    private Long resourceId;
    private String title;
    private String content;
    private String aiSummary;
    private String mindMap;
    private java.sql.Timestamp createdAt;
    private java.sql.Timestamp updatedAt;
    private String courseName;
    private String resourceTitle;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public String getMindMap() { return mindMap; }
    public void setMindMap(String mindMap) { this.mindMap = mindMap; }
    public java.sql.Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }
    public java.sql.Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.sql.Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getResourceTitle() { return resourceTitle; }
    public void setResourceTitle(String resourceTitle) { this.resourceTitle = resourceTitle; }
}
