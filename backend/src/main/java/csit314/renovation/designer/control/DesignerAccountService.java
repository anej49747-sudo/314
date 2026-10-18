package csit314.renovation.designer.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.designer.dto.CreateDesignerRequest;
import csit314.renovation.designer.dto.CreatedDesignerResponse;
import csit314.renovation.registration.exception.DuplicateEmailException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DesignerAccountService {
    private final UserAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final InitialPasswordGenerator passwords;

    public DesignerAccountService(UserAccountRepository accounts, PasswordEncoder encoder,
            InitialPasswordGenerator passwords) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.passwords = passwords;
    }

    @Transactional
    public CreatedDesignerResponse create(CreateDesignerRequest request, Authentication authentication) {
        if (authentication == null || accounts.findByEmailIgnoreCase(authentication.getName())
                .filter(account -> account.isActive() && account.getRole() == UserRole.USER_ADMIN)
                .isEmpty()) {
            throw new AccessDeniedException("Administrator access required");
        }
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }
        String initialPassword = passwords.generate();
        UserAccount account = UserAccount.newInteriorDesigner(request.salutation(), request.name().strip(),
                email, encoder.encode(initialPassword));
        try {
            UserAccount saved = accounts.saveAndFlush(account);
            return CreatedDesignerResponse.fromAccount(saved, initialPassword);
        } catch (DataIntegrityViolationException exception) {
            // The database unique index also protects concurrent requests.
            throw new DuplicateEmailException();
        }
    }
}
