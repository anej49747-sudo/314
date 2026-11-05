package csit314.renovation.favourite.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.company.entity.DesignerCompany;
import csit314.renovation.company.repository.DesignerCompanyRepository;
import csit314.renovation.favourite.entity.CustomerProjectFavourite;
import csit314.renovation.discovery.control.DiscoveryService;
import csit314.renovation.favourite.dto.CustomerFavouriteStatusResponse;
import csit314.renovation.favourite.repository.CustomerProjectFavouriteRepository;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
import java.util.HashMap;
import csit314.renovation.project.dto.PublicProjectDesignerResponse;
import csit314.renovation.project.dto.PublicProjectSearchResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import csit314.renovation.project.control.PublicProjectDetailsService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import csit314.renovation.report.repository.PlatformEngagementRecorder;

@Service
public class CustomerFavouriteService {
    private final UserAccountRepository accounts;
    private final CustomerProjectFavouriteRepository favourites;
    private final PublicProjectDetailsService publicProjects;
    private final DesignerCompanyRepository companies;
    private final ProjectImageRepository images;
    private final PlatformEngagementRecorder engagement;
    private final DiscoveryService discovery;

    public CustomerFavouriteService(UserAccountRepository accounts,
            CustomerProjectFavouriteRepository favourites, PublicProjectDetailsService publicProjects,
            DesignerCompanyRepository companies, ProjectImageRepository images, PlatformEngagementRecorder engagement,
            DiscoveryService discovery) {
        this.accounts = accounts;
        this.favourites = favourites;
        this.publicProjects = publicProjects;
        this.companies = companies;
        this.images = images;
        this.engagement = engagement;
        this.discovery = discovery;
    }

    @Transactional(readOnly = true)
    public List<PublicProjectSearchResponse> list(Authentication authentication) {
        Long customerId = requireCustomer(authentication).getId();
        List<CustomerProjectFavourite> saved = favourites.findVisibleForCustomer(
                customerId, UserRole.INTERIOR_DESIGNER);
        if (saved.isEmpty()) return List.of();

        // Resolve company display names in one batch, as in public project search.
        Map<Long, DesignerCompany> companyByDesigner = companies.findAllById(saved.stream()
                .map(entry -> entry.getProject().getDesigner().getId()).distinct().toList()).stream()
                .collect(Collectors.toMap(DesignerCompany::getDesignerId, Function.identity()));
        Map<Long, String> covers = new HashMap<>();
        images.findByProject_IdInOrderByDisplayOrderAscIdAsc(saved.stream()
                .map(entry -> entry.getProject().getId()).toList()).forEach(image ->
                covers.putIfAbsent(image.getProjectId(),
                        "/api/projects/" + image.getProjectId() + "/images/" + image.getId()));
        return saved.stream().map(entry -> {
            InteriorDesignProject project = entry.getProject();
            UserAccount designer = project.getDesigner();
            DesignerCompany company = companyByDesigner.get(designer.getId());
            String name = company == null || company.getCompanyName() == null
                    || company.getCompanyName().isBlank()
                    ? designer.getDisplayName() : company.getCompanyName();
            return new PublicProjectSearchResponse(project.getId(), project.getTitle(),
                    project.getDescription(), project.getCategoryDisplayName(), project.getStatus(),
                    new PublicProjectDesignerResponse(designer.getId(), name,
                            company == null ? null : company.getAddress()), covers.get(project.getId()), discovery.metadata(project.getId()));
        }).toList();
    }

    @Transactional(readOnly = true)
    public CustomerFavouriteStatusResponse status(Authentication authentication, Long projectId) {
        Long customerId = requireCustomer(authentication).getId();
        publicProjects.requirePublicProject(projectId);
        return new CustomerFavouriteStatusResponse(
                favourites.existsByCustomer_IdAndProject_Id(customerId, projectId));
    }

    @Transactional
    public void save(Authentication authentication, Long projectId) {
        Long customerId = requireCustomer(authentication).getId();
        publicProjects.requirePublicProject(projectId);
        if (favourites.saveIfAbsent(customerId, projectId) > 0) engagement.recordFavouriteAdded();
    }

    @Transactional
    public void remove(Authentication authentication, Long projectId) {
        Long customerId = requireCustomer(authentication).getId();
        // A customer can remove an old favourite even when its designer is now inactive.
        // Neither existence nor visibility is disclosed by this idempotent response.
        if (favourites.removeIfPresent(customerId, projectId) > 0) engagement.recordFavouriteRemoved();
    }

    private UserAccount requireCustomer(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Customer sign-in required");
        }
        return accounts.findByEmailIgnoreCase(authentication.getName())
                .filter(account -> account.isActive() && account.getRole() == UserRole.CUSTOMER)
                .orElseThrow(() -> new AccessDeniedException("Active customer access required"));
    }
}
