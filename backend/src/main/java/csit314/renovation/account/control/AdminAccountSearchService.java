package csit314.renovation.account.control;

import csit314.renovation.account.dto.AdminAccountSearchResponse;
import csit314.renovation.account.dto.AdminAccountSummaryResponse;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.authentication.control.AuthenticationService;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountSearchService {
    private static final int PAGE_SIZE = 20;
    private static final int MAX_QUERY_LENGTH = 100;
    private static final List<UserRole> ELIGIBLE_ROLES = List.of(
            UserRole.CUSTOMER, UserRole.INTERIOR_DESIGNER);

    private final UserAccountRepository accounts;
    private final AuthenticationService authenticationService;

    public AdminAccountSearchService(UserAccountRepository accounts,
            AuthenticationService authenticationService) {
        this.accounts = accounts;
        this.authenticationService = authenticationService;
    }

    @Transactional(readOnly = true)
    public AdminAccountSearchResponse search(String query, String roleFilter, String statusFilter,
            int page, Authentication authentication) {
        // Re-read the current account: a role change or deactivation must revoke an existing session.
        authenticationService.currentAdmin(authentication);
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        String normalizedQuery = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (normalizedQuery.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Search must be 100 characters or fewer");
        }
        UserRole role = parseRole(roleFilter);
        Boolean active = parseStatus(statusFilter);
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return AdminAccountSearchResponse.fromPage(accounts.searchEligibleAccounts(
                ELIGIBLE_ROLES, role, active, normalizedQuery, pageable)
                .map(AdminAccountSummaryResponse::fromAccount));
    }

    private UserRole parseRole(String roleFilter) {
        if (roleFilter == null || roleFilter.isBlank()) return null;
        return switch (roleFilter) {
            case "CUSTOMER" -> UserRole.CUSTOMER;
            case "INTERIOR_DESIGNER" -> UserRole.INTERIOR_DESIGNER;
            default -> throw new IllegalArgumentException("Invalid account role filter");
        };
    }

    private Boolean parseStatus(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) return null;
        return switch (statusFilter) {
            case "active" -> true;
            case "inactive" -> false;
            default -> throw new IllegalArgumentException("Invalid account status filter");
        };
    }
}
