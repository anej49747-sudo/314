package csit314.renovation.company.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.company.dto.CompanyInformationRequest;
import csit314.renovation.company.dto.CompanyInformationResponse;
import csit314.renovation.company.entity.DesignerCompany;
import csit314.renovation.company.repository.DesignerCompanyRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyInformationService {
    private final DesignerCompanyRepository companies;
    private final UserAccountRepository accounts;

    public CompanyInformationService(DesignerCompanyRepository companies, UserAccountRepository accounts) {
        this.companies = companies;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public CompanyInformationResponse get(Authentication authentication) {
        UserAccount designer = requireEligibleDesigner(authentication);
        return companies.findById(designer.getId())
                .map(CompanyInformationResponse::from)
                .orElseGet(CompanyInformationResponse::empty);
    }

    @Transactional
    public CompanyInformationResponse save(CompanyInformationRequest request, Authentication authentication) {
        UserAccount designer = requireEligibleDesigner(authentication);
        String name = request.companyName() == null ? "" : request.companyName().strip();
        if (name.isBlank() || name.length() > 150) {
            throw new IllegalArgumentException("Company name must contain 1–150 characters");
        }
        DesignerCompany company = companies.findById(designer.getId())
                .orElseGet(() -> new DesignerCompany(designer));
        company.updateDetails(name, optionalText(request.address()), optionalText(request.description()));
        return CompanyInformationResponse.from(companies.saveAndFlush(company));
    }

    private String optionalText(String value) {
        if (value == null || value.isBlank()) return null;
        return value.strip();
    }

    private UserAccount requireEligibleDesigner(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Interior Designer access required");
        }
        return accounts.findByEmailIgnoreCase(authentication.getName())
                .filter(account -> account.isActive() && account.getRole() == UserRole.INTERIOR_DESIGNER
                        && !account.isPasswordChangeRequired())
                .orElseThrow(() -> new AccessDeniedException("Interior Designer access required"));
    }
}
