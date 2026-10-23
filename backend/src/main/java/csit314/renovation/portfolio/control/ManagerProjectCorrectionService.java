package csit314.renovation.portfolio.control;

import csit314.renovation.account.entity.UserRole;
import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.category.entity.ProjectCategory;
import csit314.renovation.category.repository.ProjectCategoryRepository;
import csit314.renovation.portfolio.dto.ManagerProjectResponse;
import csit314.renovation.portfolio.dto.ManagerUpdateProjectRequest;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ManagerProjectCorrectionService {
    private final AuthenticationService authenticationService;
    private final InteriorDesignProjectRepository projects;
    private final ProjectCategoryRepository categories;
    private final ProjectImageRepository images;

    public ManagerProjectCorrectionService(AuthenticationService authenticationService,
            InteriorDesignProjectRepository projects, ProjectCategoryRepository categories,
            ProjectImageRepository images) {
        this.authenticationService = authenticationService;
        this.projects = projects;
        this.categories = categories;
        this.images = images;
    }

    @Transactional(readOnly = true)
    public List<ManagerProjectResponse> list(Authentication authentication) {
        authenticationService.currentManager(authentication);
        List<InteriorDesignProject> published = projects
                .findByDesigner_RoleAndDesigner_ActiveTrueOrderByCreatedAtDescIdDesc(UserRole.INTERIOR_DESIGNER);
        if (published.isEmpty()) return List.of();
        List<Long> ids = published.stream().map(InteriorDesignProject::getId).toList();
        Map<Long, Long> photoCounts = images.findByProject_IdInOrderByDisplayOrderAscIdAsc(ids).stream()
                .collect(Collectors.groupingBy(image -> image.getProjectId(), Collectors.counting()));
        return published.stream().map(project -> ManagerProjectResponse.from(project,
                photoCounts.getOrDefault(project.getId(), 0L))).toList();
    }

    @Transactional
    public ManagerProjectResponse correct(Long projectId, ManagerUpdateProjectRequest request,
            Authentication authentication) {
        authenticationService.currentManager(authentication);
        InteriorDesignProject project = projects.findById(projectId)
                .filter(p -> p.getDesigner().isActive() && p.getDesigner().getRole() == UserRole.INTERIOR_DESIGNER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        String title = request.title().strip();
        String description = request.description().strip();
        if (title.isEmpty() || description.isEmpty()) {
            throw new IllegalArgumentException("Title and description cannot be blank");
        }
        ProjectCategory category = request.categoryId() == null ? null : categories.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Choose an existing project category"));
        // No setter for owner or project images; status/title/description/category are the only edits.
        project.updateDetails(category, title, description, request.status());
        projects.flush();
        long photoCount = images.findByProject_IdInOrderByDisplayOrderAscIdAsc(List.of(projectId)).size();
        return ManagerProjectResponse.from(project, photoCount);
    }
}
