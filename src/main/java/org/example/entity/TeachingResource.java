package org.example.entity;

public class TeachingResource {
    private Long id;
    private Long courseId;
    private String title;
    private String filePath;
    private String type;
    private String chapter;
    private java.sql.Timestamp createdAt;
    private String courseName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getChapter() { return chapter; }
    public void setChapter(String chapter) { this.chapter = chapter; }
    public java.sql.Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public boolean isPdf() { return "pdf".equalsIgnoreCase(type); }
    public boolean isVideo() { return "video".equalsIgnoreCase(type); }
}
