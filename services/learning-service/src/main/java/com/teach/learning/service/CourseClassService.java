package com.teach.learning.service;
import com.teach.learning.entity.CourseClass;
import com.teach.learning.mapper.CourseClassMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class CourseClassService {
    private final CourseClassMapper classMapper;
    public CourseClassService(CourseClassMapper classMapper) { this.classMapper = classMapper; }

    public CourseClass create(Long courseId, String name, Integer maxCount) {
        CourseClass cc = new CourseClass();
        cc.setCourseId(courseId); cc.setName(name != null ? name : "默认班级");
        cc.setInviteCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        cc.setMaxCount(maxCount != null ? maxCount : 100);
        cc.setCurrentCount(0);
        classMapper.insert(cc);
        return cc;
    }

    public CourseClass findById(Long id) { return classMapper.findById(id); }
    public List<CourseClass> findByCourseId(Long courseId) { return classMapper.findByCourseId(courseId); }
    public CourseClass findByInviteCode(String inviteCode) { return classMapper.findByInviteCode(inviteCode); }
    public boolean update(Long id, String name, Integer maxCount) {
        CourseClass cc = classMapper.findById(id);
        if (cc == null) return false;
        cc.setName(name); cc.setMaxCount(maxCount);
        return classMapper.update(cc) > 0;
    }
    public boolean delete(Long id) { return classMapper.deleteById(id) > 0; }
    public void incrementCount(Long id) { classMapper.incrementCount(id); }
    public void decrementCount(Long id) { classMapper.decrementCount(id); }
}
