package csit314.renovation.account.control;

import csit314.renovation.account.dto.LoginHistoryItemResponse;
import csit314.renovation.account.dto.LoginHistoryResponse;
import csit314.renovation.account.entity.UserLoginEvent;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.account.repository.UserLoginEventRepository;
import csit314.renovation.authentication.control.AuthenticationService;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginHistoryService {
    private static final int PAGE_SIZE = 20;
    private final UserAccountRepository accounts;
    private final UserLoginEventRepository events;
    private final AuthenticationService authenticationService;

    public LoginHistoryService(UserAccountRepository accounts, UserLoginEventRepository events,
            AuthenticationService authenticationService) {
        this.accounts = accounts;
        this.events = events;
        this.authenticationService = authenticationService;
    }

    @Transactional(readOnly = true)
    public LoginHistoryResponse history(Long accountId, String outcome, int page,
            Authentication authentication) {
        authenticationService.currentAdmin(authentication);
        if (page < 0) throw new IllegalArgumentException("Invalid login history page");
        accounts.findById(accountId)
                .filter(account -> account.getRole() == UserRole.CUSTOMER
                        || account.getRole() == UserRole.INTERIOR_DESIGNER)
                .orElseThrow(AccountNotFoundException::new);
        String filter = outcome == null ? "all" : outcome.strip().toLowerCase(Locale.ROOT);
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
        Page<UserLoginEvent> found = switch (filter) {
            case "all", "" -> events.findByAccountId(accountId, pageable);
            case "success" -> events.findByAccountIdAndSuccessful(accountId, true, pageable);
            case "failed" -> events.findByAccountIdAndSuccessful(accountId, false, pageable);
            default -> throw new IllegalArgumentException("Invalid login history outcome");
        };
        return LoginHistoryResponse.fromPage(found.map(LoginHistoryItemResponse::fromEvent));
    }
}
