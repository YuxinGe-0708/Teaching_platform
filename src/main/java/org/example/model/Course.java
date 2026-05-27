package org.example.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Course {
  private Long courseId;
  private String courseCode;
  private String courseName;
  private String teacherId;
  private String description;
  private BigDecimal credit;
  private Integer hours;
  private String status;
  private List<CourseClass> classes = new ArrayList<>();

  // 构造方法
  public Course() {}

  // 添加班级
  public void addClass(CourseClass courseClass) {
    courseClass.setCourseId(this.courseId);
    this.classes.add(courseClass);
  }

  // 根据邀请码查找班级
  public CourseClass findClassByInviteCode(String inviteCode) {
    return classes.stream()
        .filter(c -> c.getInviteCode().equals(inviteCode))
        .findFirst()
        .orElse(null);
  }

  // Getters and Setters
  public Long getCourseId() { return courseId; }
  public void setCourseId(Long courseId) {
    this.courseId = courseId;
    // 更新所有班级的 courseId
    for (CourseClass c : classes) {
      c.setCourseId(courseId);
    }
  }

  public String getCourseName() { return courseName; }
  public void setCourseName(String courseName) { this.courseName = courseName; }

  public String getTeacherId() { return teacherId; }
  public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

  public String getCourseCode() { return courseCode; }
  public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

  public BigDecimal getCredit() { return credit; }
  public void setCredit(BigDecimal credit) { this.credit = credit; }

  public List<CourseClass> getClasses() { return classes; }
  public void setClasses(List<CourseClass> classes) { this.classes = classes; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public Integer getHours() { return hours; }
  public void setHours(Integer hours) { this.hours = hours; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}