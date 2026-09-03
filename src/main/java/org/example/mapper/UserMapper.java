package org.example.mapper;
import org.example.entity.User;
import java.util.List;
/** BFF port implemented by RemoteUserMapper; it never executes SQL. */
public interface UserMapper {
    User findById(Long id); User findByUsername(String username); List<User> findAll();
    List<User> findByRole(String role); List<User> findStudentsByCourseId(Long courseId);
    int countAll(); int countByRole(String role); int insert(User user); int update(User user);
    int updateByAdmin(User user); int updatePassword(Long id,String password); int deleteById(Long id);
}
