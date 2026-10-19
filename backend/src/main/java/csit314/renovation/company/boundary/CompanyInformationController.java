package csit314.renovation.company.boundary;

import csit314.renovation.company.control.CompanyInformationService;
import csit314.renovation.company.dto.CompanyInformationRequest;
import csit314.renovation.company.dto.CompanyInformationResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/designer/company-information")
public class CompanyInformationController {
    private final CompanyInformationService companyInformationService;

    public CompanyInformationController(CompanyInformationService companyInformationService) {
        this.companyInformationService = companyInformationService;
    }

    @GetMapping
    public CompanyInformationResponse get(Authentication authentication) {
        return companyInformationService.get(authentication);
    }

    @PutMapping
    public CompanyInformationResponse save(@Valid @RequestBody CompanyInformationRequest request,
            Authentication authentication) {
        return companyInformationService.save(request, authentication);
    }
}
