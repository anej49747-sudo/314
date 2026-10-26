package csit314.renovation.designer.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.company.entity.DesignerCompany;
import csit314.renovation.company.repository.DesignerCompanyRepository;
import csit314.renovation.designer.dto.DesignerProfileResponse;
import csit314.renovation.designer.dto.PublicPortfolioProjectResponse;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DesignerProfileService {
    private final UserAccountRepository accounts;
    private final DesignerCompanyRepository companies;
    private final InteriorDesignProjectRepository projects;
    private final ProjectImageRepository images;

    public DesignerProfileService(UserAccountRepository accounts, DesignerCompanyRepository companies,
            InteriorDesignProjectRepository projects, ProjectImageRepository images) {
        this.accounts = accounts;
        this.companies = companies;
        this.projects = projects;
        this.images = images;
    }

    @Transactional(readOnly = true)
    public DesignerProfileResponse findPublicProfile(Long designerId) {
        // Designer accounts are admin-provisioned; there is no separate approval flag.
        UserAccount designer = accounts.findByIdAndRoleAndActiveTrue(designerId, UserRole.INTERIOR_DESIGNER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found"));
        DesignerCompany company = companies.findById(designer.getId()).orElse(null);
        String name = company == null || company.getCompanyName() == null || company.getCompanyName().isBlank()
                ? designer.getDisplayName() : company.getCompanyName();

        List<InteriorDesignProject> portfolio = projects.findByDesigner_IdOrderByCreatedAtDescIdDesc(designer.getId());
        Map<Long, String> covers = new HashMap<>();
        if (!portfolio.isEmpty()) images.findByProject_IdInOrderByDisplayOrderAscIdAsc(
                portfolio.stream().map(InteriorDesignProject::getId).toList()).forEach(image ->
                covers.putIfAbsent(image.getProjectId(),
                        "/api/projects/" + image.getProjectId() + "/images/" + image.getId()));
        return new DesignerProfileResponse(designer.getId(), name,
                company == null ? null : company.getAddress(),
                company == null ? null : company.getDescription(),
                portfolio.stream()
                        .map(project -> PublicPortfolioProjectResponse.from(project, covers.get(project.getId())))
                        .toList());
    }
}
