package com.teach.learning.mapper;
import com.teach.learning.entity.StudyNote;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface StudyNoteMapper {
    @Select("SELECT * FROM study_note WHERE id = #{id}")
    StudyNote findById(Long id);
    @Select("SELECT * FROM study_note WHERE student_id = #{studentId} ORDER BY updated_at DESC")
    List<StudyNote> findByStudentId(Long studentId);
    @Select("SELECT * FROM study_note WHERE student_id = #{studentId} AND course_id = #{courseId} ORDER BY updated_at DESC")
    List<StudyNote> findByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    @Insert("INSERT INTO study_note (student_id, course_id, resource_id, title, content, ai_summary, mind_map) VALUES (#{studentId}, #{courseId}, #{resourceId}, #{title}, #{content}, #{aiSummary}, #{mindMap})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StudyNote note);
    @Update("UPDATE study_note SET title=#{title}, content=#{content}, ai_summary=#{aiSummary}, mind_map=#{mindMap} WHERE id=#{id}")
    int update(StudyNote note);
    @Update("UPDATE study_note SET mind_map=#{mindMap} WHERE id=#{id}")
    int updateMindMap(StudyNote note);
    @Delete("DELETE FROM study_note WHERE id=#{id}")
    int deleteById(Long id);
}
