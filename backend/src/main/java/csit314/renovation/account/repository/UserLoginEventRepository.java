package csit314.renovation.account.repository;

import csit314.renovation.account.entity.UserLoginEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLoginEventRepository extends JpaRepository<UserLoginEvent, Long> {
    Page<UserLoginEvent> findByAccountId(Long accountId, Pageable pageable);
    Page<UserLoginEvent> findByAccountIdAndSuccessful(Long accountId, boolean successful, Pageable pageable);
}
