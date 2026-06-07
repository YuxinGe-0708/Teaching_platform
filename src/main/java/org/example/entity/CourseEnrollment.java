package org.example.entity;

public class CourseEnrollment {
    private Long id;
    private Long studentId;
    private Long courseId;
    private Long classId;
    private java.sql.Timestamp enrolledAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public java.sql.Timestamp getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(java.sql.Timestamp enrolledAt) { this.enrolledAt = enrolledAt; }
}
