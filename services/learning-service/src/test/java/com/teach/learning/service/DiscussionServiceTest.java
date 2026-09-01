package com.teach.learning.service;

import com.teach.learning.entity.DiscussionPost;
import com.teach.learning.entity.DiscussionReply;
import com.teach.learning.mapper.DiscussionPostMapper;
import com.teach.learning.mapper.DiscussionReplyMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscussionServiceTest {

  @Mock
  private DiscussionPostMapper postMapper;

  @Mock
  private DiscussionReplyMapper replyMapper;

  @Mock
  private UserServiceClient userServiceClient;

  @InjectMocks
  private DiscussionService discussionService;

  @Test
  @DisplayName("UNIT-TC-DISC-01: 发起讨论帖（默认非匿名、类型 discussion）")
  void testCreatePost_DefaultValues() {
    when(postMapper.insert(any(DiscussionPost.class))).thenReturn(1);

    DiscussionPost post = discussionService.createPost(10L, 1L, "如何理解CAP定理？", "请教老师同学", null, null, null, null);

    assertNotNull(post);
    assertFalse(post.getAnonymous());
    assertEquals("discussion", post.getPostType());
    assertEquals("all", post.getTargetRole());
    verify(postMapper).insert(any(DiscussionPost.class));
  }

  @Test
  @DisplayName("UNIT-TC-DISC-02: 回复他人帖子触发跨服务站内信通知")
  void testCreateReply_NotifiesAuthor() {
    DiscussionPost authorPost = new DiscussionPost();
    authorPost.setId(100L);
    authorPost.setUserId(2L); // 发帖人是 2L

    when(replyMapper.insert(any(DiscussionReply.class))).thenReturn(1);
    when(postMapper.findById(100L)).thenReturn(authorPost);

    // 用户 3L 回复 2L 的帖子
    DiscussionReply reply = discussionService.createReply(100L, 3L, "这是我的解答", false, false);

    assertNotNull(reply);
    verify(replyMapper).insert(any(DiscussionReply.class));
    // 关键断言：必须调用 UserServiceClient 向发帖人 2L 发送通知
    verify(userServiceClient, times(1)).notify(eq(2L), eq("讨论收到新回复"), eq("这是我的解答"), eq("course"));
  }

  @Test
  @DisplayName("UNIT-TC-DISC-03: 自己回复自己帖子时不发送通知")
  void testCreateReply_SelfReplyNoNotification() {
    DiscussionPost authorPost = new DiscussionPost();
    authorPost.setId(100L);
    authorPost.setUserId(2L); // 发帖人是 2L

    when(replyMapper.insert(any(DiscussionReply.class))).thenReturn(1);
    when(postMapper.findById(100L)).thenReturn(authorPost);

    // 用户 2L 自己回复自己
    discussionService.createReply(100L, 2L, "补充说明一下", false, false);

    // 关键断言：不得发送站内信通知
    verify(userServiceClient, never()).notify(anyLong(), anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("UNIT-TC-DISC-04: 讨论帖与回复列表查询及删除")
  void testQueriesAndDelete() {
    when(postMapper.findByCourseId(10L)).thenReturn(Collections.singletonList(new DiscussionPost()));
    when(replyMapper.findByPostId(100L)).thenReturn(Collections.singletonList(new DiscussionReply()));
    when(postMapper.deleteById(100L)).thenReturn(1);
    when(replyMapper.deleteById(50L)).thenReturn(1);

    List<DiscussionPost> posts = discussionService.findByCourseId(10L);
    List<DiscussionReply> replies = discussionService.findRepliesByPostId(100L);

    assertEquals(1, posts.size());
    assertEquals(1, replies.size());
    assertTrue(discussionService.deletePost(100L));
    assertTrue(discussionService.deleteReply(50L));
  }
}