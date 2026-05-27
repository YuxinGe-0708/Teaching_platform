package org.example.model;

import java.math.BigDecimal;
import java.util.Date;

public class Submission {
  private Long submissionId;
  private Long taskId;
  private Long studentId;
  private String studentName;    // 学生姓名（展示用）
  private String content;        // 文本答案或代码
  private String fileUrl;        // 附件地址
  private String language;       // 编程语言
  private Date submitTime;
  private String submitStatus;   // 已提交/评测中/已批改/失败
  private String judgeStatus;    // AC/WA/CE/RE/TLE
  private BigDecimal score;
  private String comment;
  private Long gradedBy;
  private Date gradedTime;

  public Submission() {}

  // Getters and Setters
  public Long getSubmissionId() {
    return submissionId;
  }
  public void setSubmissionId(Long submissionId) {
    this.submissionId = submissionId;
  }

  public Long getTaskId() {
    return taskId;
  }
  public void setTaskId(Long taskId) {
    this.taskId = taskId;
  }

  public Long getStudentId() {
    return studentId;
  }
  public void setStudentId(Long studentId) {
    this.studentId = studentId;
  }

  public String getStudentName() {
    return studentName;
  }
  public void setStudentName(String studentName) {
    this.studentName = studentName;
  }

  public String getContent() {
    return content;
  }
  public void setContent(String content) {
    this.content = content;
  }

  public String getFileUrl() {
    return fileUrl;
  }
  public void setFileUrl(String fileUrl) {
    this.fileUrl = fileUrl;
  }

  public String getLanguage() {
    return language;
  }
  public void setLanguage(String language) {
    this.language = language;
  }

  public Date getSubmitTime() {
    return submitTime;
  }
  public void setSubmitTime(Date submitTime) {
    this.submitTime = submitTime;
  }

  public String getSubmitStatus() {
    return submitStatus;
  }
  public void setSubmitStatus(String submitStatus) {
    this.submitStatus = submitStatus;
  }

  public String getJudgeStatus() {
    return judgeStatus;
  }
  public void setJudgeStatus(String judgeStatus) {
    this.judgeStatus = judgeStatus;
  }

  public BigDecimal getScore() {
    return score;
  }
  public void setScore(BigDecimal score) {
    this.score = score;
  }

  public String getComment() {
    return comment;
  }
  public void setComment(String comment) {
    this.comment = comment;
  }

  public Long getGradedBy() {
    return gradedBy;
  }
  public void setGradedBy(Long gradedBy) {
    this.gradedBy = gradedBy;
  }

  public Date getGradedTime() {
    return gradedTime;
  }
  public void setGradedTime(Date gradedTime) {
    this.gradedTime = gradedTime;
  }
}
