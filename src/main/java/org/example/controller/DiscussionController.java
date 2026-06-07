package org.example.controller;

import org.example.entity.Course;
import org.example.entity.DiscussionPost;
import org.example.entity.DiscussionReply;
import org.example.entity.Notification;
import org.example.entity.User;
import org.example.mapper.CourseEnrollmentMapper;
import org.example.mapper.DiscussionMapper;
import org.example.mapper.NotificationMapper;
import org.example.service.CourseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/discussion")
public class DiscussionController {

    private final DiscussionMapper discussionMapper;
    private final CourseService courseService;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final NotificationMapper notificationMapper;

    public DiscussionController(DiscussionMapper discussionMapper,
                                CourseService courseService,
                                CourseEnrollmentMapper enrollmentMapper,
                                NotificationMapper notificationMapper) {
        this.discussionMapper = discussionMapper;
        this.courseService = courseService;
        this.enrollmentMapper = enrollmentMapper;
        this.notificationMapper = notificationMapper;
    }

    @GetMapping("/course/{courseId}")
    public String courseDiscussion(@PathVariable Long courseId, HttpSession session) {
        User user = UserController.requireUser(session);
        if (user == null) return "redirect:/login";
        if (!canAccess(user, courseId)) return "redirect:/home";
        if ("teacher".equals(user.getRole())) {
            return "redirect:/discussion/teacher/course/" + courseId;
        }
        return "redirect:/student/course/detail/" + courseId + "?tab=discussion";
    }

    @GetMapping("/teacher/course/{courseId}")
    public String teacherDiscussion(@PathVariable Long courseId, HttpSession session, Model model) {
        User user = UserController.requireUser(session);
        if (user == null || !"teacher".equals(user.getRole())) return "redirect:/login";
        Course course = courseService.findById(courseId);
        if (course == null || !user.getId().equals(course.getTeacherId())) return "redirect:/teacher/course/manage";
        model.addAttribute("user", user);
        model.addAttribute("course", UserController.toCourseView(course));
        model.addAttribute("posts", discussionMapper.findPostsByCourseId(courseId));
        return "discussion/course_discussion";
    }

    @PostMapping("/post")
    public String createPost(@RequestParam Long courseId,
                             @RequestParam String title,
                             @RequestParam String content,
                             @RequestParam(required = false, defaultValue = "discussion") String postType,
                             @RequestParam(required = false, defaultValue = "all") String targetRole,
                             @RequestParam(required = false, defaultValue = "false") Boolean anonymous,
                             HttpSession session) {
        User user = UserController.requireUser(session);
        if (user == null) return "redirect:/login";
        if (!canAccess(user, courseId)) return "redirect:/home";
        DiscussionPost post = new DiscussionPost();
        post.setCourseId(courseId);
        post.setUserId(user.getId());
        post.setTitle(title.trim());
        post.setContent(content.trim());
        post.setPostType(postType);
        post.setTargetRole(targetRole);
        post.setAnonymous(anonymous != null && anonymous);
        Course course = courseService.findById(courseId);
        if (course != null && ("teacher".equals(targetRole) || "assistant".equals(targetRole))) {
            post.setTargetUserId(course.getTeacherId());
        }
        discussionMapper.insertPost(post);
        notifyTarget(post, course, user);
        return "redirect:/discussion/post/" + post.getId();
    }

    @GetMapping("/post/{postId}")
    public String postDetail(@PathVariable Long postId, HttpSession session, Model model) {
        User user = UserController.requireUser(session);
        if (user == null) return "redirect:/login";
        DiscussionPost post = discussionMapper.findPostById(postId);
        if (post == null || !canAccess(user, post.getCourseId())) return "redirect:/home";
        Course course = courseService.findById(post.getCourseId());
        model.addAttribute("user", user);
        model.addAttribute("course", UserController.toCourseView(course));
        model.addAttribute("post", post);
        model.addAttribute("replies", discussionMapper.findRepliesByPostId(postId));
        return "discussion/post_detail";
    }

    @PostMapping("/reply")
    public String reply(@RequestParam Long postId,
                        @RequestParam String content,
                        @RequestParam(required = false, defaultValue = "false") Boolean anonymous,
                        @RequestParam(required = false, defaultValue = "false") Boolean assistantReply,
                        HttpSession session) {
        User user = UserController.requireUser(session);
        if (user == null) return "redirect:/login";
        DiscussionPost post = discussionMapper.findPostById(postId);
        if (post == null || !canAccess(user, post.getCourseId())) return "redirect:/home";
        DiscussionReply reply = new DiscussionReply();
        reply.setPostId(postId);
        reply.setUserId(user.getId());
        reply.setContent(content.trim());
        reply.setAnonymous(anonymous != null && anonymous);
        reply.setAssistantReply((assistantReply != null && assistantReply) || "teacher".equals(user.getRole()));
        discussionMapper.insertReply(reply);
        return "redirect:/discussion/post/" + postId;
    }

    private void notifyTarget(DiscussionPost post, Course course, User user) {
        if (course == null || post.getTargetUserId() == null) return;
        Notification notification = new Notification();
        notification.setUserId(post.getTargetUserId());
        notification.setType("discussion");
        notification.setTitle("课程交流区有新的定向提问");
        String author = Boolean.TRUE.equals(post.getAnonymous()) ? "匿名同学" : (user.getName() == null ? user.getUsername() : user.getName());
        notification.setContent("课程《" + course.getName() + "》收到来自 " + author + " 的帖子：" + post.getTitle());
        notificationMapper.insert(notification);
    }

    private boolean canAccess(User user, Long courseId) {
        Course course = courseService.findById(courseId);
        if (course == null) return false;
        if ("teacher".equals(user.getRole())) return user.getId().equals(course.getTeacherId());
        if ("student".equals(user.getRole())) return enrollmentMapper.findByStudentAndCourse(user.getId(), courseId) != null;
        return false;
    }
}
