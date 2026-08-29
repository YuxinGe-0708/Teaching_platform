package com.teach.learning.mapper;
import com.teach.learning.entity.DiscussionPost;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface DiscussionPostMapper {
    @Select("SELECT * FROM discussion_post WHERE id = #{id}")
    DiscussionPost findById(Long id);
    @Select("SELECT * FROM discussion_post WHERE course_id = #{courseId} ORDER BY created_at DESC")
    List<DiscussionPost> findByCourseId(Long courseId);
    @Insert("INSERT INTO discussion_post (course_id, user_id, title, content, anonymous, post_type, target_role, target_user_id) VALUES (#{courseId}, #{userId}, #{title}, #{content}, #{anonymous}, #{postType}, #{targetRole}, #{targetUserId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DiscussionPost post);
    @Delete("DELETE FROM discussion_post WHERE id=#{id}")
    int deleteById(Long id);
}
