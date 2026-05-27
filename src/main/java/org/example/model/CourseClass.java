package org.example.model;

public class CourseClass {
  private Long classId;
  private Long courseId;
  private String className;
  private String inviteCode;
  private Integer maxCount;
  private Integer currentCount;

  // 无参构造
  public CourseClass() {}

  // 完整构造方法（4个参数）
  public CourseClass(Long classId, Long courseId, String className, String inviteCode) {
    this.classId = classId;
    this.courseId = courseId;
    this.className = className;
    this.inviteCode = inviteCode;
    this.maxCount = 100;
    this.currentCount = 0;
  }

  // Getters and Setters
  public Long getClassId() { return classId; }
  public void setClassId(Long classId) { this.classId = classId; }

  public Long getCourseId() { return courseId; }
  public void setCourseId(Long courseId) { this.courseId = courseId; }

  public String getClassName() { return className; }
  public void setClassName(String className) { this.className = className; }

  public String getInviteCode() { return inviteCode; }
  public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

  public Integer getMaxCount() { return maxCount; }
  public void setMaxCount(Integer maxCount) { this.maxCount = maxCount; }

  public Integer getCurrentCount() { return currentCount; }
  public void setCurrentCount(Integer currentCount) { this.currentCount = currentCount; }
}
