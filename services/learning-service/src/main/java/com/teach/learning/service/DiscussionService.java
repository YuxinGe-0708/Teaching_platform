package com.teach.learning.service;
import com.teach.learning.entity.DiscussionPost;
import com.teach.learning.entity.DiscussionReply;
import com.teach.learning.mapper.DiscussionPostMapper;
import com.teach.learning.mapper.DiscussionReplyMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DiscussionService {
    private final DiscussionPostMapper postMapper;
    private final DiscussionReplyMapper replyMapper;
    private final UserServiceClient userServiceClient;
    public DiscussionService(DiscussionPostMapper postMapper, DiscussionReplyMapper replyMapper, UserServiceClient userServiceClient) {
        this.postMapper = postMapper; this.replyMapper = replyMapper; this.userServiceClient = userServiceClient;
    }

    public DiscussionPost createPost(Long courseId, Long userId, String title, String content, Boolean anonymous, String postType, String targetRole, Long targetUserId) {
        DiscussionPost post = new DiscussionPost();
        post.setCourseId(courseId); post.setUserId(userId); post.setTitle(title);
        post.setContent(content); post.setAnonymous(anonymous != null ? anonymous : false);
        post.setPostType(postType != null ? postType : "discussion");
        post.setTargetRole(targetRole != null ? targetRole : "all"); post.setTargetUserId(targetUserId);
        postMapper.insert(post); return post;
    }

    public DiscussionPost findPostById(Long id) { return postMapper.findById(id); }
    public List<DiscussionPost> findByCourseId(Long courseId) { return postMapper.findByCourseId(courseId); }
    public boolean deletePost(Long id) { return postMapper.deleteById(id) > 0; }

    public DiscussionReply createReply(Long postId, Long userId, String content, Boolean anonymous, Boolean assistantReply) {
        DiscussionReply reply = new DiscussionReply();
        reply.setPostId(postId); reply.setUserId(userId); reply.setContent(content);
        reply.setAnonymous(anonymous != null ? anonymous : false);
        reply.setAssistantReply(assistantReply != null ? assistantReply : false);
        replyMapper.insert(reply);
        DiscussionPost post = postMapper.findById(postId);
        if (post != null && post.getUserId() != null && !post.getUserId().equals(userId)) {
            userServiceClient.notify(post.getUserId(), "讨论收到新回复", content, "course");
        }
        return reply;
    }

    public List<DiscussionReply> findRepliesByPostId(Long postId) { return replyMapper.findByPostId(postId); }
    public boolean deleteReply(Long id) { return replyMapper.deleteById(id) > 0; }
}
