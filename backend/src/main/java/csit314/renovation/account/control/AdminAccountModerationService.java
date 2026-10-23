package csit314.renovation.account.control;

import csit314.renovation.account.dto.AdminAccountSummaryResponse;
import csit314.renovation.account.entity.AccountModerationEvent;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.AccountModerationEventRepository;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.authentication.control.AuthenticationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountModerationService {
    private final UserAccountRepository accounts;
    private final AccountModerationEventRepository events;
    private final AuthenticationService authenticationService;

    public AdminAccountModerationService(UserAccountRepository accounts,
            AccountModerationEventRepository events, AuthenticationService authenticationService) {
        this.accounts = accounts;
        this.events = events;
        this.authenticationService = authenticationService;
    }

    @Transactional
    public AdminAccountSummaryResponse suspend(Long accountId, String reason, Authentication authentication) {
        String trimmedReason = reason == null ? "" : reason.strip();
        if (trimmedReason.isEmpty() || trimmedReason.length() > 500) {
            throw new IllegalArgumentException("Suspension reason must contain 1–500 characters");
        }
        return changeStatus(accountId, false, trimmedReason, authentication);
    }

    @Transactional
    public AdminAccountSummaryResponse reactivate(Long accountId, Authentication authentication) {
        return changeStatus(accountId, true, null, authentication);
    }

    private AdminAccountSummaryResponse changeStatus(Long accountId, boolean active,
            String reason, Authentication authentication) {
        // Check the admin against current database state, not just a potentially stale session.
        var adminSession = authenticationService.currentAdmin(authentication);
        UserAccount admin = accounts.findById(adminSession.id())
                .orElseThrow(AccountNotFoundException::new);
        UserAccount target = accounts.findById(accountId)
                .filter(account -> account.getRole() == UserRole.CUSTOMER
                        || account.getRole() == UserRole.INTERIOR_DESIGNER)
                .orElseThrow(AccountNotFoundException::new);
        if (target.isActive() == active) {
            throw new AccountStatusConflictException();
        }
        target.setActiveForModeration(active);
        accounts.saveAndFlush(target);
        events.save(AccountModerationEvent.create(target, admin,
                active ? "REACTIVATE" : "SUSPEND", reason));
        return AdminAccountSummaryResponse.fromAccount(target);
    }
}
