package com.teach.learning.dto;
import com.teach.learning.entity.CourseClass;
import java.sql.Timestamp;
public class ClassView {
    private Long id; private Long courseId; private String name; private String inviteCode;
    private Integer maxCount; private Integer currentCount; private Timestamp createdAt;
    public static ClassView from(CourseClass c) {
        ClassView v = new ClassView(); v.id = c.getId(); v.courseId = c.getCourseId();
        v.name = c.getName(); v.inviteCode = c.getInviteCode(); v.maxCount = c.getMaxCount();
        v.currentCount = c.getCurrentCount(); v.createdAt = c.getCreatedAt(); return v;
    }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getCourseId() { return courseId; } public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getInviteCode() { return inviteCode; } public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public Integer getMaxCount() { return maxCount; } public void setMaxCount(Integer maxCount) { this.maxCount = maxCount; }
    public Integer getCurrentCount() { return currentCount; } public void setCurrentCount(Integer currentCount) { this.currentCount = currentCount; }
    public Timestamp getCreatedAt() { return createdAt; } public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
