package csit314.renovation.designer.boundary;

import csit314.renovation.designer.control.DesignerAccountService;
import csit314.renovation.designer.dto.CreateDesignerRequest;
import csit314.renovation.designer.dto.CreatedDesignerResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/designers")
public class DesignerAccountController {
    private final DesignerAccountService designerAccounts;

    public DesignerAccountController(DesignerAccountService designerAccounts) {
        this.designerAccounts = designerAccounts;
    }

    @PostMapping
    public ResponseEntity<CreatedDesignerResponse> create(@Valid @RequestBody CreateDesignerRequest request, Authentication authentication) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(designerAccounts.create(request, authentication));
    }
}
