package csit314.renovation.account.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.portfolio.control.ProjectImageStorage;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Physically remove eligible accounts; retain only anonymous historical report totals. */
@Service
public class PermanentAccountDeletionService {
    private final UserAccountRepository accounts;
    private final AuthenticationService authenticationService;
    private final AccountIdentitySettingsService identities;
    private final JdbcTemplate jdbc;
    private final ProjectImageStorage storage;

    public PermanentAccountDeletionService(UserAccountRepository accounts,
            AuthenticationService authenticationService, AccountIdentitySettingsService identities,
            JdbcTemplate jdbc, ProjectImageStorage storage) {
        this.accounts = accounts;
        this.authenticationService = authenticationService;
        this.identities = identities;
        this.jdbc = jdbc;
        this.storage = storage;
    }

    @Transactional
    public void deleteDesigner(Long designerId, Authentication authentication) {
        authenticationService.currentAdmin(authentication);
        UserAccount designer = accounts.findById(designerId)
                .filter(account -> account.getRole() == UserRole.INTERIOR_DESIGNER)
                .orElseThrow(AccountNotFoundException::new);
        deleteAccount(designer);
    }

    @Transactional
    public void deleteOwnCustomer(Authentication authentication) {
        UserAccount customer = identities.requireAccount(authentication, UserRole.CUSTOMER);
        deleteAccount(customer);
    }

    private void deleteAccount(UserAccount account) {
        // Keep image keys before PostgreSQL cascades remove project/image metadata.
        List<String> imageKeys = account.getRole() == UserRole.INTERIOR_DESIGNER
                ? jdbc.queryForList("SELECT i.storage_key FROM project_images i "
                        + "JOIN interior_design_projects p ON p.id = i.project_id "
                        + "WHERE p.designer_id = ?", String.class, account.getId())
                : List.of();

        preserveLostReportTotals(account);
        // Account foreign keys already cascade project, favourites, review, login and report rows.
        // The transaction commits before any physical image removal is attempted.
        accounts.delete(account);
        accounts.flush();
        if (!imageKeys.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    imageKeys.forEach(storage::deleteQuietly);
                }
            });
        }
    }

    /** Summarise only rows that will be lost; no account or session IDs survive this operation. */
    private void preserveLostReportTotals(UserAccount account) {
        Long id = account.getId();
        for (String period : List.of("DAY", "WEEK", "MONTH")) {
            // Period is a fixed server-defined token; it never comes from an HTTP request.
            String dateExpression = periodDate(period, "e.occurred_at");
            aggregate(period, "LOGINS", "SELECT " + dateExpression + " AS period_start, COUNT(*) AS total "
                    + "FROM user_login_events e WHERE e.account_id = ? AND e.successful = true "
                    + "GROUP BY period_start", id);
            aggregate(period, "UNIQUE_USERS", "SELECT DISTINCT " + dateExpression
                    + " AS period_start, 1 AS total FROM user_login_events e "
                    + "WHERE e.account_id = ? AND e.successful = true", id);
            aggregate(period, "NEW_ACCOUNTS", "SELECT " + periodDate(period, "a.created_at")
                    + " AS period_start, 1 AS total FROM user_accounts a WHERE a.id = ?", id);
            aggregate(period, "REVIEWS", "SELECT " + periodDate(period, "r.created_at")
                    + " AS period_start, COUNT(*) AS total FROM designer_reviews r "
                    + "WHERE r.customer_id = ? OR r.designer_id = ? GROUP BY period_start", id, id);
        }
    }

    private String periodDate(String period, String timestamp) {
        String local = "(" + timestamp + " AT TIME ZONE 'Asia/Singapore')";
        return switch (period) {
            case "DAY" -> local + "::date";
            case "WEEK" -> "date_trunc('week', " + local + ")::date";
            case "MONTH" -> "date_trunc('month', " + local + ")::date";
            default -> throw new IllegalArgumentException("Invalid period");
        };
    }

    private void aggregate(String period, String metric, String query, Object... params) {
        // Insert or add only aggregate counts. No deleted account identifier is stored.
        String sql = "INSERT INTO deleted_account_activity_totals(period_type, period_start, metric, total) "
                + "SELECT ?, src.period_start, ?, src.total FROM (" + query + ") src WHERE TRUE "
                + "ON CONFLICT (period_type, period_start, metric) DO UPDATE SET total = "
                + "deleted_account_activity_totals.total + EXCLUDED.total";
        Object[] args = new Object[params.length + 2];
        args[0] = period;
        args[1] = metric;
        System.arraycopy(params, 0, args, 2, params.length);
        jdbc.update(sql, args);
    }
}
