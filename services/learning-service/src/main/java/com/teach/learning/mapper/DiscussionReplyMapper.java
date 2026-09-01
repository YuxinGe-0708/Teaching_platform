package com.teach.learning.mapper;
import com.teach.learning.entity.DiscussionReply;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface DiscussionReplyMapper {
    @Select("SELECT * FROM discussion_reply WHERE post_id = #{postId} ORDER BY created_at ASC")
    List<DiscussionReply> findByPostId(Long postId);
    @Insert("INSERT INTO discussion_reply (post_id, user_id, content, anonymous, assistant_reply) VALUES (#{postId}, #{userId}, #{content}, #{anonymous}, #{assistantReply})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DiscussionReply reply);
    @Delete("DELETE FROM discussion_reply WHERE id=#{id}")
    int deleteById(Long id);
}
