package com.teach.learning.service;
import com.teach.learning.entity.Course;
import com.teach.learning.mapper.CourseMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class CourseService {
    private final CourseMapper courseMapper;
    public CourseService(CourseMapper courseMapper) { this.courseMapper = courseMapper; }

    public Course create(Long teacherId, String name, String code, String description, Integer credits, String subjectCategory, Integer hours, Boolean allowJoin, String status) {
        Course course = new Course();
        course.setTeacherId(teacherId); course.setName(name); course.setCode(code);
        course.setDescription(description); course.setCredits(credits);
        course.setSubjectCategory(subjectCategory); course.setHours(hours);
        course.setAllowJoin(allowJoin != null ? allowJoin : true);
        course.setStatus(normalizeStatus(status));
        course.setInviteCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        courseMapper.insert(course);
        return course;
    }

    public Course findById(Long id) { return courseMapper.findById(id); }
    public List<Course> getTeacherCourses(Long teacherId) { return courseMapper.findByTeacherId(teacherId); }
    public List<Course> getStudentCourses(Long studentId) { return courseMapper.findByStudentId(studentId); }
    public List<Course> getActiveCourses() { return courseMapper.findActive(); }
    public Course findByInviteCode(String inviteCode) { return courseMapper.findByInviteCode(inviteCode); }

    public boolean canAccess(Long courseId, Long userId, String action) {
        Course course = courseMapper.findById(courseId);
        if (course == null || userId == null) return false;
        if (userId.equals(course.getTeacherId())) return true;
        return "enroll".equalsIgnoreCase(action) && "active".equals(course.getStatus()) && Boolean.TRUE.equals(course.getAllowJoin());
    }

    public boolean update(Long id, Long teacherId, String name, String code, String description, Integer credits, String subjectCategory, Integer hours, Boolean allowJoin, String status) {
        Course course = courseMapper.findById(id);
        if (course == null || !course.getTeacherId().equals(teacherId)) return false;
        course.setName(name); course.setCode(code); course.setDescription(description);
        course.setCredits(credits); course.setSubjectCategory(subjectCategory);
        course.setHours(hours); course.setAllowJoin(allowJoin);
        course.setStatus(normalizeStatus(status));
        return courseMapper.update(course) > 0;
    }

    public boolean delete(Long id, Long teacherId) {
        Course course = courseMapper.findById(id);
        if (course == null || !course.getTeacherId().equals(teacherId)) return false;
        return courseMapper.deleteById(id) > 0;
    }

    public boolean archive(Long id, Long teacherId) {
        Course course = courseMapper.findById(id);
        if (course == null || !course.getTeacherId().equals(teacherId)) return false;
        courseMapper.updateStatus(id, "archived");
        return true;
    }

    private String normalizeStatus(String status) {
        if ("draft".equals(status) || "closed".equals(status) || "archived".equals(status)) return status;
        return "active";
    }
}
