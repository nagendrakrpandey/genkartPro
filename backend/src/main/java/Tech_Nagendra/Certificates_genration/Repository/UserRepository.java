package Tech_Nagendra.Certificates_genration.Repository;

import Tech_Nagendra.Certificates_genration.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}

