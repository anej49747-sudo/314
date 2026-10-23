package csit314.renovation.authentication.control;

import csit314.renovation.account.entity.UserLoginEvent;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.account.repository.UserLoginEventRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Write outcome without a credential, raw IP address, session ID, or existence-disclosing response. */
@Service
public class LoginAttemptRecorder {
    private final UserAccountRepository accounts;
    private final UserLoginEventRepository events;

    public LoginAttemptRecorder(UserAccountRepository accounts, UserLoginEventRepository events) {
        this.accounts = accounts;
        this.events = events;
    }

    @Transactional
    public void record(String email, boolean successful) {
        accounts.findByEmailIgnoreCase(email.strip().toLowerCase(Locale.ROOT))
                .ifPresent(account -> events.save(UserLoginEvent.attempt(account, successful)));
    }
}
