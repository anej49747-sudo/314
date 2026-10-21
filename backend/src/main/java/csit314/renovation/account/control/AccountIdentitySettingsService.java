package csit314.renovation.account.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.registration.exception.DuplicateEmailException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** Shared name/email rules for Stories #14 and #21. Transaction is owned by the caller. */
@Service
public class AccountIdentitySettingsService {
    private final UserAccountRepository accounts;

    public AccountIdentitySettingsService(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    public UserAccount requireAccount(Authentication authentication, UserRole role) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Account access required");
        }
        return accounts.findByEmailIgnoreCase(authentication.getName())
                .filter(account -> account.isActive() && account.getRole() == role
                        && (role != UserRole.INTERIOR_DESIGNER || !account.isPasswordChangeRequired()))
                .orElseThrow(() -> new AccessDeniedException("Account access required"));
    }

    /** Updates only name/email; returns true if the email-based session must be ended. */
    public boolean update(UserAccount account, String requestedName, String requestedEmail) {
        String name = requestedName.strip();
        String email = requestedEmail.strip().toLowerCase(Locale.ROOT);
        if (name.isEmpty() || name.length() > 160) {
            throw new IllegalArgumentException("Display name must contain 1–160 characters");
        }
        if (email.isEmpty() || email.length() > 254) {
            throw new IllegalArgumentException("Email must contain 1–254 characters");
        }
        boolean emailChanged = !account.getEmail().equalsIgnoreCase(email);
        if (emailChanged && accounts.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }
        account.updateAccountIdentity(name, email);
        try {
            // Database unique index also handles concurrent, case-insensitive collisions.
            accounts.saveAndFlush(account);
        } catch (DataIntegrityViolationException ignored) {
            throw new DuplicateEmailException();
        }
        return emailChanged;
    }
}
