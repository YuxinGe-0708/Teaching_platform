package org.example.bff;
import org.example.entity.*; import org.example.mapper.DiscussionMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component; import org.springframework.context.annotation.Primary; import java.util.List;
@Primary @Component @ConditionalOnProperty(name="app.bff.enabled",havingValue="true")
public class RemoteDiscussionMapper implements DiscussionMapper {
 private final MicroserviceClient c; public RemoteDiscussionMapper(MicroserviceClient c){this.c=c;}
 public List<DiscussionPost> findPostsByCourseId(Long id){List<DiscussionPost> v=c.getList(c.uri(c.learning("/internal/bff/posts")).queryParam("courseId",id).toUriString(),DiscussionPost.class);v.forEach(this::enrich);return v;}
 public DiscussionPost findPostById(Long id){return enrich(c.get(c.learning("/internal/bff/posts/"+id),DiscussionPost.class));}
 public int insertPost(DiscussionPost v){DiscussionPost saved=c.post(c.learning("/internal/bff/posts"),v,DiscussionPost.class);if(saved!=null)v.setId(saved.getId());return saved==null?0:1;}
 public List<DiscussionReply> findRepliesByPostId(Long id){List<DiscussionReply> v=c.getList(c.uri(c.learning("/internal/bff/replies")).queryParam("postId",id).toUriString(),DiscussionReply.class);v.forEach(this::enrich);return v;}
 public int insertReply(DiscussionReply v){DiscussionReply saved=c.post(c.learning("/internal/bff/replies"),v,DiscussionReply.class);if(saved!=null)v.setId(saved.getId());return saved==null?0:1;}
 private DiscussionPost enrich(DiscussionPost v){if(v!=null){User u=user(v.getUserId());if(u!=null){v.setAuthorName(u.getName()==null?u.getUsername():u.getName());v.setAuthorRole(u.getRole());}}return v;}
 private DiscussionReply enrich(DiscussionReply v){if(v!=null){User u=user(v.getUserId());if(u!=null){v.setAuthorName(u.getName()==null?u.getUsername():u.getName());v.setAuthorRole(u.getRole());}}return v;}
 private User user(Long id){return id==null?null:c.get(c.user("/internal/bff/users/"+id),User.class);}
}
