package org.example.service;

import org.example.entity.Course;
import org.example.entity.ResourceProgress;
import org.example.entity.Submission;
import org.example.entity.Task;
import org.example.entity.TeachingResource;
import org.example.entity.User;
import org.example.mapper.CourseMapper;
import org.example.mapper.ResourceProgressMapper;
import org.example.mapper.SubmissionMapper;
import org.example.mapper.TaskMapper;
import org.example.mapper.TeachingResourceMapper;
import org.example.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoreService {

    private final CourseMapper courseMapper;
    private final TaskMapper taskMapper;
    private final SubmissionMapper submissionMapper;
    private final UserMapper userMapper;
    private final TeachingResourceMapper resourceMapper;
    private final ResourceProgressMapper progressMapper;

    public ScoreService(CourseMapper courseMapper,
                        TaskMapper taskMapper,
                        SubmissionMapper submissionMapper,
                        UserMapper userMapper,
                        TeachingResourceMapper resourceMapper,
                        ResourceProgressMapper progressMapper) {
        this.courseMapper = courseMapper;
        this.taskMapper = taskMapper;
        this.submissionMapper = submissionMapper;
        this.userMapper = userMapper;
        this.resourceMapper = resourceMapper;
        this.progressMapper = progressMapper;
    }

    public List<Map<String, Object>> studentScoreSummary(Long studentId) {
        return courseMapper.findByStudentId(studentId).stream()
                .map(course -> studentCourseScore(studentId, course.getId()))
                .collect(Collectors.toList());
    }

    public Map<String, Object> studentCourseScore(Long studentId, Long courseId) {
        Course course = courseMapper.findById(courseId);
        List<Task> tasks = taskMapper.findByCourseId(courseId);
        Set<Long> courseTaskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        List<Submission> submissions = submissionMapper.findByStudentId(studentId).stream()
                .filter(s -> s.getTaskId() != null && courseTaskIds.contains(s.getTaskId()))
                .collect(Collectors.toList());
        Map<Long, Submission> byTask = submissions.stream()
                .filter(s -> s.getTaskId() != null)
                .collect(Collectors.toMap(Submission::getTaskId, s -> s, (a, b) -> a));

        List<Map<String, Object>> taskRows = new ArrayList<>();
        for (Task task : tasks) {
            Submission submission = byTask.get(task.getId());
            taskRows.add(taskScoreRow(task, submission));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("courseId", courseId);
        summary.put("courseName", course == null ? "未知课程" : course.getName());
        summary.put("homeworkScore", categoryAverage(tasks, byTask, "homework"));
        summary.put("examScore", categoryAverage(tasks, byTask, "exam"));
        summary.put("practiceScore", categoryAverage(tasks, byTask, "programming"));
        summary.put("finalScore", finalScore(tasks, byTask));
        summary.put("submittedCount", tasks.stream().filter(t -> byTask.containsKey(t.getId())).count());
        summary.put("taskCount", tasks.size());
        summary.put("taskRows", taskRows);
        summary.put("learningProgress", learningProgress(studentId, courseId));
        return summary;
    }

    public Map<String, Object> teacherCourseStatistics(Long courseId) {
        Course course = courseMapper.findById(courseId);
        List<Task> tasks = taskMapper.findByCourseId(courseId);
        List<User> students = userMapper.findStudentsByCourseId(courseId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (User student : students) {
            Map<String, Object> row = studentCourseScore(student.getId(), courseId);
            row.put("studentId", student.getId());
            row.put("studentName", student.getName() == null ? student.getUsername() : student.getName());
            row.put("username", student.getUsername());
            rows.add(row);
        }
        rows.sort((a, b) -> Double.compare(asDouble(b.get("finalScore")), asDouble(a.get("finalScore"))));
        int rank = 1;
        for (Map<String, Object> row : rows) {
            row.put("rank", rank++);
        }

        double avg = rows.stream().mapToDouble(r -> asDouble(r.get("finalScore"))).average().orElse(0);
        double passRate = rows.isEmpty() ? 0 : rows.stream().filter(r -> asDouble(r.get("finalScore")) >= 60).count() * 100.0 / rows.size();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("courseId", courseId);
        stats.put("courseName", course == null ? "未知课程" : course.getName());
        stats.put("taskCount", tasks.size());
        stats.put("studentCount", students.size());
        stats.put("averageScore", round(avg));
        stats.put("passRate", round(passRate));
        stats.put("rows", rows);
        stats.put("bands", scoreBands(rows));
        stats.put("taskTitles", tasks.stream().map(Task::getTitle).collect(Collectors.toList()));
        return stats;
    }

    public Map<String, Object> learningProgress(Long studentId, Long courseId) {
        List<Task> tasks = taskMapper.findByCourseId(courseId);
        Set<Long> courseTaskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        Set<Long> submittedTaskIds = submissionMapper.findByStudentId(studentId).stream()
                .map(Submission::getTaskId)
                .filter(taskId -> taskId != null && courseTaskIds.contains(taskId))
                .collect(Collectors.toSet());
        long finishedTasks = tasks.stream().filter(t -> submittedTaskIds.contains(t.getId())).count();
        double taskProgress = tasks.isEmpty() ? 100 : finishedTasks * 100.0 / tasks.size();

        List<TeachingResource> resources = resourceMapper.findByCourseId(courseId);
        Map<Long, ResourceProgress> progressMap = progressMapper.findByStudentAndCourse(studentId, courseId).stream()
                .collect(Collectors.toMap(ResourceProgress::getResourceId, p -> p, (a, b) -> a));
        double resourceProgress = resources.isEmpty()
                ? 100
                : resources.stream().mapToDouble(r -> progressMap.containsKey(r.getId()) ? safe(progressMap.get(r.getId()).getProgress()) : 0).average().orElse(0);

        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("taskProgress", round(taskProgress));
        progress.put("resourceProgress", round(resourceProgress));
        progress.put("overallProgress", round((taskProgress + resourceProgress) / 2));
        progress.put("finishedTasks", finishedTasks);
        progress.put("taskCount", tasks.size());
        progress.put("resources", resources.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", r.getTitle());
            item.put("type", r.getType());
            item.put("progress", round(progressMap.containsKey(r.getId()) ? safe(progressMap.get(r.getId()).getProgress()) : 0));
            return item;
        }).collect(Collectors.toList()));
        progress.put("tasks", tasks.stream().map(t -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", t.getTitle());
            item.put("type", t.getType());
            item.put("completed", submittedTaskIds.contains(t.getId()));
            return item;
        }).collect(Collectors.toList()));
        return progress;
    }

    private Map<String, Object> taskScoreRow(Task task, Submission submission) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("taskId", task.getId());
        row.put("title", task.getTitle());
        row.put("type", task.getType());
        row.put("typeName", displayTaskType(task.getType()));
        row.put("maxScore", task.getMaxScore() == null ? 100 : task.getMaxScore());
        row.put("submitted", submission != null);
        row.put("status", submission == null ? "未提交" : submission.getSubmitStatus());
        row.put("score", submission == null || submission.getScore() == null ? null : round(submission.getScore()));
        row.put("judgeStatus", submission == null ? "-" : submission.getJudgeStatus());
        row.put("feedback", submission == null ? "" : submission.getFeedback());
        return row;
    }

    private double categoryAverage(List<Task> tasks, Map<Long, Submission> byTask, String type) {
        List<Task> typed = tasks.stream().filter(t -> type.equals(t.getType())).collect(Collectors.toList());
        if (typed.isEmpty()) return 0;
        return round(typed.stream().mapToDouble(t -> normalizedScore(t, byTask.get(t.getId()))).average().orElse(0));
    }

    private double finalScore(List<Task> tasks, Map<Long, Submission> byTask) {
        if (tasks.isEmpty()) return 0;
        return round(tasks.stream().mapToDouble(t -> normalizedScore(t, byTask.get(t.getId()))).average().orElse(0));
    }

    private double normalizedScore(Task task, Submission submission) {
        if (submission == null || submission.getScore() == null) return 0;
        double max = task.getMaxScore() == null || task.getMaxScore() <= 0 ? 100 : task.getMaxScore();
        return Math.min(100, submission.getScore() * 100.0 / max);
    }

    private List<Map<String, Object>> scoreBands(List<Map<String, Object>> rows) {
        int[] counts = new int[5];
        for (Map<String, Object> row : rows) {
            double score = asDouble(row.get("finalScore"));
            if (score < 60) counts[0]++;
            else if (score < 70) counts[1]++;
            else if (score < 80) counts[2]++;
            else if (score < 90) counts[3]++;
            else counts[4]++;
        }
        String[] labels = {"0-59", "60-69", "70-79", "80-89", "90-100"};
        List<Map<String, Object>> bands = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            Map<String, Object> band = new LinkedHashMap<>();
            band.put("label", labels[i]);
            band.put("count", counts[i]);
            bands.add(band);
        }
        return bands;
    }

    private double safe(Double value) {
        return value == null ? 0 : value;
    }

    private String displayTaskType(String type) {
        if ("exam".equals(type)) return "考试";
        if ("programming".equals(type)) return "编程实训";
        return "实验作业";
    }

    private double asDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0D;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
