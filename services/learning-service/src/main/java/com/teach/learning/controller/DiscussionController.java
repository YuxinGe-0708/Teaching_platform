package com.teach.learning.controller;
import com.teach.learning.dto.ApiResponse;
import com.teach.learning.dto.DiscussionPostView;
import com.teach.learning.dto.DiscussionReplyView;
import com.teach.learning.entity.DiscussionPost;
import com.teach.learning.entity.DiscussionReply;
import com.teach.learning.service.DiscussionService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/discussions")
public class DiscussionController {
    private final DiscussionService discussionService;
    public DiscussionController(DiscussionService discussionService) { this.discussionService = discussionService; }

    @GetMapping("/posts/course/{courseId}")
    public ApiResponse<List<DiscussionPostView>> postsByCourse(@PathVariable Long courseId) {
        return ApiResponse.ok(discussionService.findByCourseId(courseId).stream().map(DiscussionPostView::from).collect(Collectors.toList()));
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<DiscussionPostView> getPost(@PathVariable Long id) {
        DiscussionPost post = discussionService.findPostById(id);
        if (post == null) return ApiResponse.fail(404, "帖子不存在");
        return ApiResponse.ok(DiscussionPostView.from(post));
    }

    @PostMapping("/posts")
    public ApiResponse<DiscussionPostView> createPost(@RequestParam Long courseId, @RequestParam Long userId,
                                                       @RequestParam String title, @RequestParam String content,
                                                       @RequestParam(required = false, defaultValue = "false") Boolean anonymous,
                                                       @RequestParam(required = false, defaultValue = "discussion") String postType,
                                                       @RequestParam(required = false, defaultValue = "all") String targetRole,
                                                       @RequestParam(required = false) Long targetUserId) {
        return ApiResponse.ok(DiscussionPostView.from(discussionService.createPost(courseId, userId, title, content, anonymous, postType, targetRole, targetUserId)));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<?> deletePost(@PathVariable Long id) {
        return discussionService.deletePost(id) ? ApiResponse.ok("删除成功") : ApiResponse.fail(404, "帖子不存在");
    }

    @GetMapping("/replies/{postId}")
    public ApiResponse<List<DiscussionReplyView>> repliesByPost(@PathVariable Long postId) {
        return ApiResponse.ok(discussionService.findRepliesByPostId(postId).stream().map(DiscussionReplyView::from).collect(Collectors.toList()));
    }

    @PostMapping("/replies")
    public ApiResponse<DiscussionReplyView> createReply(@RequestParam Long postId, @RequestParam Long userId,
                                                         @RequestParam String content,
                                                         @RequestParam(required = false, defaultValue = "false") Boolean anonymous,
                                                         @RequestParam(required = false, defaultValue = "false") Boolean assistantReply) {
        return ApiResponse.ok(DiscussionReplyView.from(discussionService.createReply(postId, userId, content, anonymous, assistantReply)));
    }

    @DeleteMapping("/replies/{id}")
    public ApiResponse<?> deleteReply(@PathVariable Long id) {
        return discussionService.deleteReply(id) ? ApiResponse.ok("删除成功") : ApiResponse.fail(404, "回复不存在");
    }
}
