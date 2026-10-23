package csit314.renovation.account.boundary;

import csit314.renovation.account.control.PermanentAccountDeletionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accounts")
public class AdminDesignerDeletionController {
    private final PermanentAccountDeletionService deletion;

    public AdminDesignerDeletionController(PermanentAccountDeletionService deletion) {
        this.deletion = deletion;
    }

    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDesigner(@PathVariable Long accountId, Authentication authentication) {
        deletion.deleteDesigner(accountId, authentication);
    }
}
