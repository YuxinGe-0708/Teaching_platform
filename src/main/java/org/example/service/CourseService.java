package org.example.service;

import org.example.entity.Course;
import org.example.entity.CourseClass;
import org.example.entity.CourseEnrollment;
import org.example.mapper.CourseClassMapper;
import org.example.mapper.CourseMapper;
import org.example.mapper.CourseEnrollmentMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final CourseClassMapper courseClassMapper;

    public CourseService(CourseMapper courseMapper, CourseEnrollmentMapper enrollmentMapper, CourseClassMapper courseClassMapper) {
        this.courseMapper = courseMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.courseClassMapper = courseClassMapper;
    }

    public Course createCourse(Course course) {
        course.setInviteCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        if (course.getStatus() == null || course.getStatus().trim().isEmpty()) {
            course.setStatus("active");
        }
        if (course.getAllowJoin() == null) {
            course.setAllowJoin(Boolean.TRUE);
        }
        courseMapper.insert(course);
        CourseClass defaultClass = new CourseClass();
        defaultClass.setCourseId(course.getId());
        defaultClass.setName("默认班级");
        defaultClass.setInviteCode(course.getInviteCode());
        defaultClass.setMaxCount(100);
        defaultClass.setCurrentCount(0);
        courseClassMapper.insert(defaultClass);
        return course;
    }

    public Course findById(Long id) {
        return courseMapper.findById(id);
    }

    public List<Course> getTeacherCourses(Long teacherId) {
        return distinctCourses(courseMapper.findByTeacherId(teacherId));
    }

    public List<Course> searchTeacherCourses(Long teacherId, String keyword, String sort) {
        return distinctCourses(courseMapper.searchTeacherCourses(teacherId, keyword == null ? "" : keyword.trim(), sort));
    }

    public List<Course> getStudentCourses(Long studentId) {
        return distinctCourses(courseMapper.findByStudentId(studentId));
    }

    public List<Course> getAllActiveCourses() {
        return courseMapper.findAllActive();
    }

    public boolean enroll(Long studentId, Long courseId) {
        Course course = courseMapper.findById(courseId);
        if (course == null || !"active".equals(course.getStatus()) || !course.getAllowJoin()) return false;

        CourseEnrollment existing = enrollmentMapper.findByStudentAndCourse(studentId, courseId);
        if (existing != null) return false;

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        List<CourseClass> classes = courseClassMapper.findByCourseId(courseId);
        if (!classes.isEmpty()) {
            enrollment.setClassId(classes.get(0).getId());
        }
        enrollmentMapper.insert(enrollment);
        if (!classes.isEmpty()) {
            courseClassMapper.incrementCount(classes.get(0).getId());
        }
        return true;
    }

    public boolean enrollByInviteCode(Long studentId, String inviteCode) {
        CourseClass courseClass = courseClassMapper.findByInviteCode(inviteCode);
        Course course = courseClass == null ? courseMapper.findByInviteCode(inviteCode) : courseMapper.findById(courseClass.getCourseId());
        if (course == null || !"active".equals(course.getStatus()) || !course.getAllowJoin()) return false;

        CourseEnrollment existing = enrollmentMapper.findByStudentAndCourse(studentId, course.getId());
        if (existing != null) return false;

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(course.getId());
        enrollment.setClassId(courseClass == null ? null : courseClass.getId());
        enrollmentMapper.insert(enrollment);
        if (courseClass != null) {
            courseClassMapper.incrementCount(courseClass.getId());
        }
        return true;
    }

    public boolean unenroll(Long studentId, Long courseId) {
        int rows = enrollmentMapper.delete(studentId, courseId);
        if (rows > 0) {
            List<CourseClass> classes = courseClassMapper.findByCourseId(courseId);
            if (!classes.isEmpty()) {
                courseClassMapper.decrementCount(classes.get(0).getId());
            }
        }
        return rows > 0;
    }

    public Course updateCourse(Course course) {
        courseMapper.update(course);
        return course;
    }

    public void updateStatus(Long id, String status) {
        courseMapper.updateStatus(id, status);
    }

    public void deleteCourse(Long id) {
        courseMapper.delete(id);
    }

    private List<Course> distinctCourses(List<Course> courses) {
        Map<Long, Course> unique = new LinkedHashMap<>();
        for (Course course : courses) {
            if (course == null || course.getId() == null) continue;
            unique.putIfAbsent(course.getId(), course);
        }
        return new ArrayList<>(unique.values());
    }
}
