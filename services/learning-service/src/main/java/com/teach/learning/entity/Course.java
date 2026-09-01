package com.teach.learning.entity;
import java.sql.Timestamp;
public class Course {
    private Long id; private String name; private String code; private String description;
    private Integer credits; private String subjectCategory; private Integer hours;
    private Long teacherId; private String inviteCode; private Boolean allowJoin;
    private String status; private Timestamp createdAt; private Timestamp updatedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getCode() { return code; } public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public Integer getCredits() { return credits; } public void setCredits(Integer credits) { this.credits = credits; }
    public String getSubjectCategory() { return subjectCategory; } public void setSubjectCategory(String subjectCategory) { this.subjectCategory = subjectCategory; }
    public Integer getHours() { return hours; } public void setHours(Integer hours) { this.hours = hours; }
    public Long getTeacherId() { return teacherId; } public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    public String getInviteCode() { return inviteCode; } public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public Boolean getAllowJoin() { return allowJoin; } public void setAllowJoin(Boolean allowJoin) { this.allowJoin = allowJoin; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; } public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
