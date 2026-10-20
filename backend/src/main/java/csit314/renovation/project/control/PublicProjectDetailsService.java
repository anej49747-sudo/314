package csit314.renovation.project.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.company.entity.DesignerCompany;
import csit314.renovation.company.repository.DesignerCompanyRepository;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import csit314.renovation.project.dto.PublicProjectDesignerResponse;
import csit314.renovation.project.dto.PublicProjectDetailsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicProjectDetailsService {
    private final InteriorDesignProjectRepository projects;
    private final DesignerCompanyRepository companies;

    public PublicProjectDetailsService(InteriorDesignProjectRepository projects,
            DesignerCompanyRepository companies) {
        this.projects = projects;
        this.companies = companies;
    }

    @Transactional(readOnly = true)
    public PublicProjectDetailsResponse findPublicProject(Long projectId) {
        InteriorDesignProject project = projects.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found"));
        UserAccount designer = project.getDesigner();
        // Suspended designer profiles must not be exposed through their projects either.
        if (!designer.isActive() || designer.getRole() != UserRole.INTERIOR_DESIGNER) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        DesignerCompany company = companies.findById(designer.getId()).orElse(null);
        String name = company == null || company.getCompanyName() == null
                || company.getCompanyName().isBlank()
                ? designer.getDisplayName() : company.getCompanyName();

        return new PublicProjectDetailsResponse(project.getId(), project.getTitle(),
                project.getDescription(), project.getCategory().getName(), project.getStatus(),
                new PublicProjectDesignerResponse(designer.getId(), name,
                        company == null ? null : company.getAddress()));
    }
}
