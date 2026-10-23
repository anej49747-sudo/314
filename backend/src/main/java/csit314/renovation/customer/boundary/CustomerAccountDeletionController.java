package csit314.renovation.customer.boundary;

import csit314.renovation.account.control.PermanentAccountDeletionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/account")
public class CustomerAccountDeletionController {
    private final PermanentAccountDeletionService deletion;

    public CustomerAccountDeletionController(PermanentAccountDeletionService deletion) {
        this.deletion = deletion;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOwnAccount(Authentication authentication, HttpServletRequest request) {
        deletion.deleteOwnCustomer(authentication); // commits before ending the session
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
    }
}
