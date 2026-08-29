package com.teach.learning.entity;
import java.sql.Timestamp;
public class CourseEnrollment {
    private Long id; private Long studentId; private Long courseId; private Long classId;
    private Timestamp enrolledAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; } public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCourseId() { return courseId; } public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getClassId() { return classId; } public void setClassId(Long classId) { this.classId = classId; }
    public Timestamp getEnrolledAt() { return enrolledAt; } public void setEnrolledAt(Timestamp enrolledAt) { this.enrolledAt = enrolledAt; }
}
