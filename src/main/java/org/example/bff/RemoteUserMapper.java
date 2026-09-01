package org.example.bff;

import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component; import org.springframework.context.annotation.Primary;

import java.util.List;

@Primary @Component
@ConditionalOnProperty(name="app.bff.enabled",havingValue="true")
public class RemoteUserMapper implements UserMapper {
    private final MicroserviceClient c;
    public RemoteUserMapper(MicroserviceClient c){this.c=c;}
    public User findById(Long id){return c.get(c.user("/internal/bff/users/"+id),User.class);}
    public User findByUsername(String username){return c.get(c.uri(c.user("/internal/bff/users/by-username")).queryParam("username",username).toUriString(),User.class);}
    public List<User> findAll(){return c.getList(c.user("/internal/bff/users"),User.class);}
    public List<User> findByRole(String role){return c.getList(c.uri(c.user("/internal/bff/users")).queryParam("role",role).toUriString(),User.class);}
    public List<User> findStudentsByCourseId(Long courseId){return c.getList(c.user("/internal/users/course/"+courseId),User.class);}
    public int countAll(){Integer n=c.get(c.user("/internal/bff/users/count"),Integer.class);return n==null?0:n;}
    public int countByRole(String role){Integer n=c.get(c.uri(c.user("/internal/bff/users/count")).queryParam("role",role).toUriString(),Integer.class);return n==null?0:n;}
    public int insert(User user){User saved=c.post(c.user("/internal/bff/users"),user,User.class);if(saved!=null)user.setId(saved.getId());return saved==null?0:1;}
    public int update(User user){return number(c.put(c.user("/internal/bff/users/"+user.getId()+"/profile"),user,Integer.class));}
    public int updateByAdmin(User user){return number(c.put(c.user("/internal/bff/users/"+user.getId()+"/admin"),user,Integer.class));}
    public int updatePassword(Long id,String password){return number(c.put(c.uri(c.user("/internal/bff/users/"+id+"/password")).queryParam("password",password).toUriString(),null,Integer.class));}
    public int deleteById(Long id){return number(c.delete(c.user("/internal/bff/users/"+id),Integer.class));}
    private int number(Integer n){return n==null?0:n;}
}
