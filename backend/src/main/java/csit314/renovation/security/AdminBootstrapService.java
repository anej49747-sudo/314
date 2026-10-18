package csit314.renovation.security;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.registration.validation.RegistrationPasswordValidator;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapService {
    private static final Pattern EMAIL_FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final UserAccountRepository accounts;
    private final PasswordEncoder encoder;

    public AdminBootstrapService(UserAccountRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    @Transactional
    public void ensureCreated(String configuredEmail, String configuredPassword) {
        if (configuredEmail == null || configuredPassword == null) {
            throw new IllegalStateException("Admin bootstrap requires email and password configuration");
        }
        String email = configuredEmail.strip().toLowerCase(Locale.ROOT);
        if (email.length() > 254 || !EMAIL_FORMAT.matcher(email).matches()
                || !new RegistrationPasswordValidator().isValid(configuredPassword, null)) {
            throw new IllegalStateException("Admin bootstrap configuration is invalid");
        }
        accounts.findByEmailIgnoreCase(email).ifPresentOrElse(existing -> {
            if (existing.getRole() != UserRole.USER_ADMIN || !existing.isActive()) {
                // Never promote an existing account or reactivate a disabled administrator.
                throw new IllegalStateException("Admin bootstrap account conflicts with an existing account");
            }
        }, () -> accounts.saveAndFlush(UserAccount.newUserAdmin("Local Administrator", email,
                encoder.encode(configuredPassword))));
    }
}
