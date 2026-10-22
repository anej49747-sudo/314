package csit314.renovation.account.boundary;

import csit314.renovation.account.control.AdminAccountSearchService;
import csit314.renovation.account.dto.AdminAccountSearchResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountSearchController {
    private final AdminAccountSearchService searchService;

    public AdminAccountSearchController(AdminAccountSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<AdminAccountSearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(searchService.search(q, role, status, page, authentication));
    }
}
