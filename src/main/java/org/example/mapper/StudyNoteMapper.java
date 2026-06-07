package org.example.mapper;

import org.apache.ibatis.annotations.*;
import org.example.entity.StudyNote;

import java.util.List;

@Mapper
public interface StudyNoteMapper {

    @Select("SELECT n.*, c.name AS course_name, r.title AS resource_title FROM study_note n "
            + "LEFT JOIN course c ON n.course_id = c.id "
            + "LEFT JOIN resource r ON n.resource_id = r.id "
            + "WHERE n.student_id = #{studentId} ORDER BY n.updated_at DESC")
    List<StudyNote> findByStudentId(Long studentId);

    @Select("SELECT n.*, c.name AS course_name, r.title AS resource_title FROM study_note n "
            + "LEFT JOIN course c ON n.course_id = c.id "
            + "LEFT JOIN resource r ON n.resource_id = r.id "
            + "WHERE n.student_id = #{studentId} AND n.course_id = #{courseId} ORDER BY n.updated_at DESC")
    List<StudyNote> findByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Select("SELECT n.*, c.name AS course_name, r.title AS resource_title FROM study_note n "
            + "LEFT JOIN course c ON n.course_id = c.id "
            + "LEFT JOIN resource r ON n.resource_id = r.id "
            + "WHERE n.id = #{id}")
    StudyNote findById(Long id);

    @Insert("INSERT INTO study_note (student_id, course_id, resource_id, title, content, ai_summary, mind_map) "
            + "VALUES (#{studentId}, #{courseId}, #{resourceId}, #{title}, #{content}, #{aiSummary}, #{mindMap})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StudyNote note);

    @Update("UPDATE study_note SET course_id=#{courseId}, resource_id=#{resourceId}, title=#{title}, content=#{content}, "
            + "ai_summary=#{aiSummary}, mind_map=#{mindMap}, updated_at=NOW() WHERE id=#{id} AND student_id=#{studentId}")
    int update(StudyNote note);

    @Update("UPDATE study_note SET mind_map=#{mindMap}, updated_at=NOW() WHERE id=#{id} AND student_id=#{studentId}")
    int updateMindMap(StudyNote note);

    @Delete("DELETE FROM study_note WHERE id = #{id} AND student_id = #{studentId}")
    int deleteByStudent(@Param("id") Long id, @Param("studentId") Long studentId);
}
