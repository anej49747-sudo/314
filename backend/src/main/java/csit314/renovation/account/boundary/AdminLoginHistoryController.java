package csit314.renovation.account.boundary;

import csit314.renovation.account.control.LoginHistoryService;
import csit314.renovation.account.dto.LoginHistoryResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accounts")
public class AdminLoginHistoryController {
    private final LoginHistoryService history;

    public AdminLoginHistoryController(LoginHistoryService history) {
        this.history = history;
    }

    @GetMapping("/{accountId}/login-history")
    public ResponseEntity<LoginHistoryResponse> history(@PathVariable Long accountId,
            @RequestParam(defaultValue = "all") String outcome,
            @RequestParam(defaultValue = "0") int page, Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(history.history(accountId, outcome, page, authentication));
    }
}
