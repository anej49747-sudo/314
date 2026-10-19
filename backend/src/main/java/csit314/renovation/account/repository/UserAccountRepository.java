package csit314.renovation.account.repository;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    List<UserAccount> findByRoleAndActiveTrue(UserRole role);
}
