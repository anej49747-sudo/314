package csit314.renovation.designer.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.company.entity.DesignerCompany;
import csit314.renovation.company.repository.DesignerCompanyRepository;
import csit314.renovation.designer.dto.DesignerProfileResponse;
import csit314.renovation.designer.dto.PublicPortfolioProjectResponse;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DesignerProfileService {
    private final UserAccountRepository accounts;
    private final DesignerCompanyRepository companies;
    private final InteriorDesignProjectRepository projects;

    public DesignerProfileService(UserAccountRepository accounts, DesignerCompanyRepository companies,
            InteriorDesignProjectRepository projects) {
        this.accounts = accounts;
        this.companies = companies;
        this.projects = projects;
    }

    @Transactional(readOnly = true)
    public DesignerProfileResponse findPublicProfile(Long designerId) {
        // Designer accounts are admin-provisioned; there is no separate approval flag.
        UserAccount designer = accounts.findByIdAndRoleAndActiveTrue(designerId, UserRole.INTERIOR_DESIGNER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found"));
        DesignerCompany company = companies.findById(designer.getId()).orElse(null);
        String name = company == null || company.getCompanyName() == null || company.getCompanyName().isBlank()
                ? designer.getDisplayName() : company.getCompanyName();

        return new DesignerProfileResponse(designer.getId(), name,
                company == null ? null : company.getAddress(),
                company == null ? null : company.getDescription(),
                projects.findByDesigner_IdOrderByCreatedAtDescIdDesc(designer.getId()).stream()
                        .map(PublicPortfolioProjectResponse::from).toList());
    }
}
