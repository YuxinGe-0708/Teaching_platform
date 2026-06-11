package org.example.service;

import org.example.entity.ExamRecord;
import org.example.entity.Submission;
import org.example.entity.Task;
import org.example.mapper.ExamRecordMapper;
import org.example.mapper.SubmissionMapper;
import org.example.util.ExamContentUtils;
import org.example.util.TaskMetadataUtils;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class ExamService {

    private final ExamRecordMapper examRecordMapper;
    private final SubmissionMapper submissionMapper;

    public ExamService(ExamRecordMapper examRecordMapper, SubmissionMapper submissionMapper) {
        this.examRecordMapper = examRecordMapper;
        this.submissionMapper = submissionMapper;
    }

    public ExamRecord getExamRecord(Long studentId, Long taskId) {
        return examRecordMapper.findByStudentAndTask(studentId, taskId);
    }

    public ExamRecord beginExam(Long studentId, Long taskId) {
        ExamRecord record = examRecordMapper.findByStudentAndTask(studentId, taskId);
        if (record != null) {
            if (record.isSubmitted()) return record;
            if (record.isInProgress()) return record;
            if (record.isNotStarted()) {
                record.setStartTime(new Timestamp(System.currentTimeMillis()));
                record.setStatus("IN_PROGRESS");
                examRecordMapper.beginExam(record);
                return record;
            }
        }
        record = new ExamRecord();
        record.setTaskId(taskId);
        record.setStudentId(studentId);
        record.setStartTime(new Timestamp(System.currentTimeMillis()));
        record.setContent("");
        record.setStatus("IN_PROGRESS");
        examRecordMapper.insert(record);
        return record;
    }

    public ExamRecord submitExam(Long studentId, Long taskId, String content) {
        return doSubmit(studentId, taskId, content, "SUBMITTED");
    }

    public ExamRecord autoSubmitExam(Long studentId, Long taskId, String content) {
        return doSubmit(studentId, taskId, content, "AUTO_SUBMITTED");
    }

    public ExamRecord saveProgress(Long studentId, Long taskId, String content) {
        ExamRecord record = examRecordMapper.findByStudentAndTask(studentId, taskId);
        if (record == null || record.isSubmitted()) return record;
        record.setContent(content != null ? content : "");
        examRecordMapper.updateContent(record);
        return record;
    }

    public long getRemainingSeconds(ExamRecord record, Task task) {
        if (record == null || record.getStartTime() == null) return 0;
        if (task.getEndTime() == null) return 7200;
        long deadline = task.getEndTime().getTime();
        long now = System.currentTimeMillis();
        long remaining = (deadline - now) / 1000;
        return Math.max(0, remaining);
    }

    public boolean isExamTimeUp(ExamRecord record, Task task) {
        return getRemainingSeconds(record, task) <= 0;
    }

    private ExamRecord doSubmit(Long studentId, Long taskId, String content, String status) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        ExamRecord record = examRecordMapper.findByStudentAndTask(studentId, taskId);
        if (record == null) {
            record = new ExamRecord();
            record.setTaskId(taskId);
            record.setStudentId(studentId);
            record.setStartTime(now);
            record.setContent(content != null ? content : "");
            record.setSubmitTime(now);
            record.setStatus(status);
            examRecordMapper.insert(record);
            examRecordMapper.submit(record);
            return record;
        }
        if (record.isSubmitted()) return record;
        record.setContent(content != null ? content : "");
        record.setSubmitTime(now);
        record.setStatus(status);
        examRecordMapper.submit(record);
        return record;
    }

    public void createSubmissionFromExam(ExamRecord record, Task task) {
        if (record == null || task == null) return;
        Submission submission = submissionMapper.findByStudentAndTask(record.getStudentId(), record.getTaskId());
        String content = record.getContent() != null ? record.getContent() : "";
        boolean autoGraded = submission != null && "graded".equals(submission.getStatus())
                && submission.getFeedback() != null && submission.getFeedback().startsWith("系统自动判分");
        boolean teacherGraded = submission != null && "graded".equals(submission.getStatus()) && !autoGraded;

        if (submission == null) {
            submission = new Submission();
            submission.setTaskId(record.getTaskId());
            submission.setStudentId(record.getStudentId());
            submission.setContent(content);
            submission.setFilePath("");
            submission.setStatus("submitted");
            submission.setJudgeResult("");
            submissionMapper.insert(submission);
        } else if (!teacherGraded) {
            submission.setContent(content);
            submissionMapper.updateContent(submission);
        }

        if (!teacherGraded) {
            autoGradeExam(task, submission, content);
        }
        record.setScore(submission.getScore());
        if (record.getSubmitTime() == null) {
            record.setSubmitTime(new Timestamp(System.currentTimeMillis()));
        }
        if (record.getStatus() == null || !record.isSubmitted()) {
            record.setStatus("SUBMITTED");
        }
        examRecordMapper.submit(record);
    }

    private void autoGradeExam(Task task, Submission submission, String content) {
        String answer = TaskMetadataUtils.examAnswer(task.getDescription());
        if (answer == null || answer.trim().isEmpty()) return;
        String submittedAnswer = ExamContentUtils.firstAnswerText(content);
        boolean correct = answer.trim().equalsIgnoreCase(submittedAnswer.trim());
        submission.setScore(correct ? Double.valueOf(task.getMaxScore() == null ? 100 : task.getMaxScore()) : 0D);
        submission.setStatus("graded");
        submission.setJudgeResult(correct ? "AC" : "WA");
        submission.setFeedback(correct ? "系统自动判分：答案正确。" : "系统自动判分：答案与标准答案不一致。");
        submissionMapper.grade(submission);
    }
}
