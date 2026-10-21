package csit314.renovation.project.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.company.entity.DesignerCompany;
import csit314.renovation.company.repository.DesignerCompanyRepository;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import csit314.renovation.project.dto.PublicProjectDesignerResponse;
import csit314.renovation.project.dto.PublicProjectSearchResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicProjectSearchService {
    private final InteriorDesignProjectRepository projects;
    private final DesignerCompanyRepository companies;

    public PublicProjectSearchService(InteriorDesignProjectRepository projects,
            DesignerCompanyRepository companies) {
        this.projects = projects;
        this.companies = companies;
    }

    @Transactional(readOnly = true)
    public List<PublicProjectSearchResponse> findProjects(String titleQuery, Long categoryId) {
        String keyword = titleQuery == null ? "" : titleQuery.trim();
        List<InteriorDesignProject> results;
        if (categoryId == null) {
            results = keyword.isEmpty()
                    ? projects.findByDesigner_RoleAndDesigner_ActiveTrueOrderByCreatedAtDescIdDesc(
                            UserRole.INTERIOR_DESIGNER)
                    : projects.findByDesigner_RoleAndDesigner_ActiveTrueAndTitleContainingIgnoreCaseOrderByCreatedAtDescIdDesc(
                            UserRole.INTERIOR_DESIGNER, keyword);
        } else {
            results = keyword.isEmpty()
                    ? projects.findByDesigner_RoleAndDesigner_ActiveTrueAndCategory_IdOrderByCreatedAtDescIdDesc(
                            UserRole.INTERIOR_DESIGNER, categoryId)
                    : projects.findByDesigner_RoleAndDesigner_ActiveTrueAndCategory_IdAndTitleContainingIgnoreCaseOrderByCreatedAtDescIdDesc(
                            UserRole.INTERIOR_DESIGNER, categoryId, keyword);
        }
        if (results.isEmpty()) return List.of();

        // One batch query instead of a company lookup for each project card.
        Map<Long, DesignerCompany> byDesignerId = companies.findAllById(results.stream()
                .map(project -> project.getDesigner().getId()).distinct().toList()).stream()
                .collect(Collectors.toMap(DesignerCompany::getDesignerId, Function.identity()));

        return results.stream().map(project -> {
            UserAccount designer = project.getDesigner();
            DesignerCompany company = byDesignerId.get(designer.getId());
            String designerName = company == null || company.getCompanyName() == null
                    || company.getCompanyName().isBlank()
                    ? designer.getDisplayName() : company.getCompanyName();
            return new PublicProjectSearchResponse(project.getId(), project.getTitle(),
                    project.getDescription(), project.getCategory().getName(), project.getStatus(),
                    new PublicProjectDesignerResponse(designer.getId(), designerName,
                            company == null ? null : company.getAddress()));
        }).toList();
    }
}
