package com.teach.learning.entity;
import java.sql.Timestamp;
public class Resource {
    private Long id; private Long courseId; private String title; private String filePath;
    private String type; private String chapter; private Long fileSize; private Integer downloadCount;
    private Timestamp createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getCourseId() { return courseId; } public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public String getFilePath() { return filePath; } public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getType() { return type; } public void setType(String type) { this.type = type; }
    public String getChapter() { return chapter; } public void setChapter(String chapter) { this.chapter = chapter; }
    public Long getFileSize() { return fileSize; } public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Integer getDownloadCount() { return downloadCount; } public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }
    public Timestamp getCreatedAt() { return createdAt; } public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
