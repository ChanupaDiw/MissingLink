package dao;

import model.User;
import java.util.List;

public interface UserDAO {
    User findByUsername(String username);
    User findById(int id);
    void save(User user, String plainPassword);
    List<User> findPendingAdmins();
    void approve(int userId);
}
