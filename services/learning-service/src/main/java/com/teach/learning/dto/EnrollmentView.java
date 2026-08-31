package com.teach.learning.dto;
import com.teach.learning.entity.CourseEnrollment;
import java.sql.Timestamp;
public class EnrollmentView {
    private Long id; private Long studentId; private Long courseId; private Long classId; private Timestamp enrolledAt;
    public static EnrollmentView from(CourseEnrollment e) {
        EnrollmentView v = new EnrollmentView(); v.id = e.getId(); v.studentId = e.getStudentId();
        v.courseId = e.getCourseId(); v.classId = e.getClassId(); v.enrolledAt = e.getEnrolledAt(); return v;
    }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; } public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCourseId() { return courseId; } public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getClassId() { return classId; } public void setClassId(Long classId) { this.classId = classId; }
    public Timestamp getEnrolledAt() { return enrolledAt; } public void setEnrolledAt(Timestamp enrolledAt) { this.enrolledAt = enrolledAt; }
}
