package csit314.renovation.design.boundary;

import csit314.renovation.design.control.DesignDraftService;
import csit314.renovation.design.dto.DesignDraftCreateRequest;
import csit314.renovation.design.dto.DesignDraftResponse;
import csit314.renovation.design.dto.DesignDraftSaveRequest;
import csit314.renovation.design.dto.DesignDraftSummaryResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/designs")
public class DesignDraftController {
    private final DesignDraftService service;
    public DesignDraftController(DesignDraftService service) { this.service = service; }

    @GetMapping public List<DesignDraftSummaryResponse> list(Authentication auth) { return service.list(auth); }
    @GetMapping("/{id}") public DesignDraftResponse get(Authentication auth, @PathVariable Long id) {
        return service.get(auth, id);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public DesignDraftResponse create(Authentication auth, @RequestBody DesignDraftCreateRequest body) {
        return service.create(auth, body);
    }
    @PutMapping("/{id}") public DesignDraftResponse save(Authentication auth, @PathVariable Long id,
            @RequestBody DesignDraftSaveRequest body) { return service.save(auth, id, body); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) { service.delete(auth, id); }
}
