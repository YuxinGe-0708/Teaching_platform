package org.example.service;

import org.example.entity.Submission;
import org.example.entity.Task;
import org.example.mapper.SubmissionMapper;
import org.example.mapper.TaskMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final SubmissionMapper submissionMapper;

    public TaskService(TaskMapper taskMapper, SubmissionMapper submissionMapper) {
        this.taskMapper = taskMapper;
        this.submissionMapper = submissionMapper;
    }

    public Task createTask(Task task) {
        task.setStatus("published");
        taskMapper.insert(task);
        return task;
    }

    public Task findById(Long id) {
        return taskMapper.findById(id);
    }

    public List<Task> getCourseTasks(Long courseId) {
        return taskMapper.findByCourseId(courseId);
    }

    public List<Task> getStudentTasks(Long studentId) {
        return taskMapper.findByStudentId(studentId);
    }

    public Task updateTask(Task task) {
        taskMapper.update(task);
        return task;
    }

    public void deleteTask(Long id) {
        taskMapper.delete(id);
    }

    public Submission submit(Long taskId, Long studentId, String content) {
        Submission existing = submissionMapper.findByStudentAndTask(studentId, taskId);
        if (existing != null) {
            existing.setContent(content);
            submissionMapper.updateContent(existing);
            return existing;
        }
        Submission submission = new Submission();
        submission.setTaskId(taskId);
        submission.setStudentId(studentId);
        submission.setContent(content);
        submission.setStatus("submitted");
        submissionMapper.insert(submission);
        return submission;
    }

    public Submission getSubmission(Long studentId, Long taskId) {
        return submissionMapper.findByStudentAndTask(studentId, taskId);
    }

    public List<Submission> getStudentSubmissions(Long studentId) {
        return submissionMapper.findByStudentId(studentId);
    }
}
