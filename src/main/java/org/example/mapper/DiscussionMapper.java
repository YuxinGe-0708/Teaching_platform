package org.example.mapper;
import org.example.entity.DiscussionPost;
import org.example.entity.DiscussionReply;
import java.util.List;
public interface DiscussionMapper {
    List<DiscussionPost> findPostsByCourseId(Long courseId); DiscussionPost findPostById(Long id); int insertPost(DiscussionPost value);
    List<DiscussionReply> findRepliesByPostId(Long postId); int insertReply(DiscussionReply value);
}
