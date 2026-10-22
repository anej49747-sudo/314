package csit314.renovation.account.boundary;

import csit314.renovation.account.control.AdminAccountModerationService;
import csit314.renovation.account.dto.AdminAccountSummaryResponse;
import csit314.renovation.account.dto.SuspendAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountModerationController {
    private final AdminAccountModerationService moderation;

    public AdminAccountModerationController(AdminAccountModerationService moderation) {
        this.moderation = moderation;
    }

    @PutMapping("/{accountId}/suspend")
    public ResponseEntity<AdminAccountSummaryResponse> suspend(@PathVariable Long accountId,
            @Valid @RequestBody SuspendAccountRequest request, Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(moderation.suspend(accountId, request.reason(), authentication));
    }

    @PutMapping("/{accountId}/reactivate")
    public ResponseEntity<AdminAccountSummaryResponse> reactivate(@PathVariable Long accountId,
            Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(moderation.reactivate(accountId, authentication));
    }
}
