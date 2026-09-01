package org.example.mapper;
import org.example.entity.Task;
import java.util.List;
public interface TaskMapper {
    Task findById(Long id); List<Task> findByCourseId(Long courseId); List<Task> findByStudentId(Long studentId);
    int countAll(); int insert(Task value); int update(Task value); int updateStatus(Long id,String status); int delete(Long id);
}
