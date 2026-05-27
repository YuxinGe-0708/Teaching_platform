package org.example.service;

import org.example.entity.Course;
import org.example.entity.CourseEnrollment;
import org.example.mapper.CourseMapper;
import org.example.mapper.CourseEnrollmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper enrollmentMapper;

    public CourseService(CourseMapper courseMapper, CourseEnrollmentMapper enrollmentMapper) {
        this.courseMapper = courseMapper;
        this.enrollmentMapper = enrollmentMapper;
    }

    public Course createCourse(Course course) {
        course.setInviteCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        course.setStatus("active");
        courseMapper.insert(course);
        return course;
    }

    public Course findById(Long id) {
        return courseMapper.findById(id);
    }

    public List<Course> getTeacherCourses(Long teacherId) {
        return courseMapper.findByTeacherId(teacherId);
    }

    public List<Course> getStudentCourses(Long studentId) {
        return courseMapper.findByStudentId(studentId);
    }

    public List<Course> getAllActiveCourses() {
        return courseMapper.findAllActive();
    }

    @Transactional
    public boolean enrollByInviteCode(Long studentId, String inviteCode) {
        Course course = courseMapper.findByInviteCode(inviteCode);
        if (course == null) return false;

        CourseEnrollment existing = enrollmentMapper.findByStudentAndCourse(studentId, course.getId());
        if (existing != null) return false;

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(course.getId());
        enrollmentMapper.insert(enrollment);
        return true;
    }

    @Transactional
    public boolean unenroll(Long studentId, Long courseId) {
        int rows = enrollmentMapper.delete(studentId, courseId);
        return rows > 0;
    }

    public Course updateCourse(Course course) {
        courseMapper.update(course);
        return course;
    }

    public void deleteCourse(Long id) {
        courseMapper.delete(id);
    }
}
