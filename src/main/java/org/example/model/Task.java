package org.example.model;

import java.math.BigDecimal;
import java.util.Date;

public class Task {
  private Long taskId;
  private Long courseId;
  private Long classId;
  private Long teacherId;
  private String title;
  private String description;
  private String taskType;   // 作业/考试/编程实训
  private String questionConfig;  // 题目、选项、标准答案、测试用例等JSON
  private Date startTime;
  private Date endTime;
  private BigDecimal fullScore;
  private BigDecimal weight;
  private String status;  // 草稿/已发布/已关闭

  // ========== 所有 Getter 和 Setter ==========

  public Long getTaskId() { return taskId; }
  public void setTaskId(Long taskId) { this.taskId = taskId; }

  public Long getCourseId() { return courseId; }
  public void setCourseId(Long courseId) { this.courseId = courseId; }

  public Long getClassId() { return classId; }
  public void setClassId(Long classId) { this.classId = classId; }

  public Long getTeacherId() { return teacherId; }
  public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getTaskType() { return taskType; }
  public void setTaskType(String taskType) { this.taskType = taskType; }

  public String getQuestionConfig() { return questionConfig; }
  public void setQuestionConfig(String questionConfig) { this.questionConfig = questionConfig; }

  public Date getStartTime() { return startTime; }
  public void setStartTime(Date startTime) { this.startTime = startTime; }

  public Date getEndTime() { return endTime; }
  public void setEndTime(Date endTime) { this.endTime = endTime; }

  public BigDecimal getFullScore() { return fullScore; }
  public void setFullScore(BigDecimal fullScore) { this.fullScore = fullScore; }

  public BigDecimal getWeight() { return weight; }
  public void setWeight(BigDecimal weight) { this.weight = weight; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
