package org.example.entity;

public class ResourceProgress {
    private Long id;
    private Long studentId;
    private Long resourceId;
    private Double progress;
    private Double lastPosition;
    private Double duration;
    private java.sql.Timestamp updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public Double getProgress() { return progress; }
    public void setProgress(Double progress) { this.progress = progress; }
    public Double getLastPosition() { return lastPosition; }
    public void setLastPosition(Double lastPosition) { this.lastPosition = lastPosition; }
    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }
    public java.sql.Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.sql.Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
