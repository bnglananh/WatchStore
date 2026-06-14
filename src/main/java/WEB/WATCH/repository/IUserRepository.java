package WEB.WATCH.repository;
import WEB.WATCH.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface IUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.enabled = :enabled WHERE u.id = :id")
    void updateEnabledStatus(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("enabled") boolean enabled);
}
