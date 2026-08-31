package com.teach.assessment.service;

import com.teach.assessment.entity.Submission;
import com.teach.assessment.entity.Task;
import com.teach.assessment.mapper.SubmissionMapper;
import com.teach.assessment.mapper.TaskMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final SubmissionMapper submissionMapper;

    public TaskService(TaskMapper taskMapper, SubmissionMapper submissionMapper) {
        this.taskMapper = taskMapper;
        this.submissionMapper = submissionMapper;
    }

    public Task createTask(Task task) {
        if (task.getStatus() == null || task.getStatus().trim().isEmpty()) {
            task.setStatus("published");
        }
        taskMapper.insert(task);
        return task;
    }

    public Task findById(Long id) {
        return taskMapper.findById(id);
    }

    public List<Task> getCourseTasks(Long courseId) {
        return distinctTasks(taskMapper.findByCourseId(courseId));
    }

    public List<Task> getStudentTasks(Long studentId) {
        return distinctTasks(taskMapper.findByStudentId(studentId));
    }
    public List<Task> getPublishedTasks() { return distinctTasks(taskMapper.findPublished()); }

    public Task updateTask(Task task) {
        taskMapper.update(task);
        return task;
    }

    public void deleteTask(Long id) {
        taskMapper.delete(id);
    }

    public void updateStatus(Long id, String status) {
        taskMapper.updateStatus(id, status);
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

    private List<Task> distinctTasks(List<Task> tasks) {
        Map<Long, Task> unique = new LinkedHashMap<>();
        for (Task task : tasks) {
            if (task == null || task.getId() == null) continue;
            unique.putIfAbsent(task.getId(), task);
        }
        return new ArrayList<>(unique.values());
    }
}
