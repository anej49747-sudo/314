package csit314.renovation.designer.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.designer.dto.DesignerAccountSettingsRequest;
import csit314.renovation.designer.dto.DesignerAccountSettingsResponse;
import csit314.renovation.registration.exception.DuplicateEmailException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DesignerAccountSettingsService {
    private final UserAccountRepository accounts;

    public DesignerAccountSettingsService(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public DesignerAccountSettingsResponse get(Authentication authentication) {
        return DesignerAccountSettingsResponse.from(requireDesigner(authentication), false);
    }

    @Transactional
    public DesignerAccountSettingsResponse update(DesignerAccountSettingsRequest request,
            Authentication authentication) {
        UserAccount designer = requireDesigner(authentication);
        String name = request.displayName().strip();
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (name.isEmpty() || name.length() > 160) {
            throw new IllegalArgumentException("Display name must contain 1–160 characters");
        }
        if (email.isEmpty() || email.length() > 254) {
            throw new IllegalArgumentException("Email must contain 1–254 characters");
        }
        boolean emailChanged = !designer.getEmail().equalsIgnoreCase(email);
        if (emailChanged && accounts.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }
        designer.updateAccountIdentity(name, email);
        try {
            // Flush checks the DB's case-insensitive unique index even under concurrent updates.
            accounts.saveAndFlush(designer);
        } catch (DataIntegrityViolationException ignored) {
            throw new DuplicateEmailException();
        }
        return DesignerAccountSettingsResponse.from(designer, emailChanged);
    }

    private UserAccount requireDesigner(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Interior Designer access required");
        }
        return accounts.findByEmailIgnoreCase(authentication.getName())
                .filter(account -> account.isActive() && account.getRole() == UserRole.INTERIOR_DESIGNER
                        && !account.isPasswordChangeRequired())
                .orElseThrow(() -> new AccessDeniedException("Interior Designer access required"));
    }
}
