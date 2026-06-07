package org.example.mapper;

import org.apache.ibatis.annotations.*;
import org.example.entity.DiscussionPost;
import org.example.entity.DiscussionReply;

import java.util.List;

@Mapper
public interface DiscussionMapper {

    @Select("SELECT p.*, u.name AS author_name, u.role AS author_role FROM discussion_post p LEFT JOIN user u ON p.user_id = u.id WHERE p.course_id = #{courseId} ORDER BY p.created_at DESC")
    List<DiscussionPost> findPostsByCourseId(Long courseId);

    @Select("SELECT p.*, u.name AS author_name, u.role AS author_role FROM discussion_post p LEFT JOIN user u ON p.user_id = u.id WHERE p.id = #{id}")
    DiscussionPost findPostById(Long id);

    @Insert("INSERT INTO discussion_post (course_id, user_id, title, content, anonymous, post_type, target_role, target_user_id) "
            + "VALUES (#{courseId}, #{userId}, #{title}, #{content}, #{anonymous}, #{postType}, #{targetRole}, #{targetUserId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPost(DiscussionPost post);

    @Select("SELECT r.*, u.name AS author_name, u.role AS author_role FROM discussion_reply r LEFT JOIN user u ON r.user_id = u.id WHERE r.post_id = #{postId} ORDER BY r.created_at ASC")
    List<DiscussionReply> findRepliesByPostId(Long postId);

    @Insert("INSERT INTO discussion_reply (post_id, user_id, content, anonymous, assistant_reply) "
            + "VALUES (#{postId}, #{userId}, #{content}, #{anonymous}, #{assistantReply})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReply(DiscussionReply reply);
}
