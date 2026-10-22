package csit314.renovation.account.repository;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    List<UserAccount> findByRoleAndActiveTrue(UserRole role);
    Optional<UserAccount> findByIdAndRoleAndActiveTrue(Long id, UserRole role);

    // LOCATE treats the submitted search as literal text, not a SQL LIKE wildcard pattern.
    @Query("""
            SELECT account FROM UserAccount account
            WHERE account.role IN :eligibleRoles
              AND (:role IS NULL OR account.role = :role)
              AND (:active IS NULL OR account.active = :active)
              AND (:query = '' OR LOCATE(:query, LOWER(account.name)) > 0
                   OR LOCATE(:query, LOWER(account.email)) > 0)
            """)
    Page<UserAccount> searchEligibleAccounts(@Param("eligibleRoles") List<UserRole> eligibleRoles,
            @Param("role") UserRole role, @Param("active") Boolean active,
            @Param("query") String query, Pageable pageable);
}
