package csit314.renovation.registration.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.registration.dto.RegisterRequest;
import csit314.renovation.registration.dto.RegisterResponse;
import csit314.renovation.registration.exception.DuplicateEmailException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserAccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }
        UserAccount account = UserAccount.newCustomer(request.salutation(), request.name().strip(),
                email, passwordEncoder.encode(request.password()));
        try {
            // Flush inside the service so concurrent unique-index failures yield HTTP 409.
            return RegisterResponse.fromAccount(accounts.saveAndFlush(account));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }
}
