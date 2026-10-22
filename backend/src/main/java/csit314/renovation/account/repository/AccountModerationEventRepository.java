package csit314.renovation.account.repository;

import csit314.renovation.account.entity.AccountModerationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountModerationEventRepository extends JpaRepository<AccountModerationEvent, Long> {}
