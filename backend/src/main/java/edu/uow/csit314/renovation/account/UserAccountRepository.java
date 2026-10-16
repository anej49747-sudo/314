package edu.uow.csit314.renovation.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<UserAccount> findByEmailIgnoreCase(String email);
}
