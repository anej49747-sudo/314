package csit314.renovation.portfolio.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.category.repository.ProjectCategoryRepository;
import csit314.renovation.metrics.repository.ProjectInterestMetricsRepository;
import csit314.renovation.portfolio.dto.CreateProjectRequest;
import csit314.renovation.portfolio.dto.UpdateProjectRequest;
import csit314.renovation.portfolio.dto.ProjectImageResponse;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectImage;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
import csit314.renovation.portfolio.dto.PortfolioProjectResponse;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {
    private final InteriorDesignProjectRepository projects;
    private final UserAccountRepository accounts;
    private final ProjectCategoryRepository categories;
    private final ProjectImageRepository images;
    private final ProjectImageStorage storage;
    private final ProjectInterestMetricsRepository interestMetrics;
    private final csit314.renovation.discovery.control.DiscoveryService discovery;

    public PortfolioService(InteriorDesignProjectRepository projects, UserAccountRepository accounts,
            ProjectCategoryRepository categories, ProjectImageRepository images,
            ProjectImageStorage storage, ProjectInterestMetricsRepository interestMetrics,
            csit314.renovation.discovery.control.DiscoveryService discovery) {
        this.projects = projects;
        this.accounts = accounts;
        this.categories = categories;
        this.images = images;
        this.storage = storage;
        this.interestMetrics = interestMetrics;
        this.discovery = discovery;
    }

    @Transactional(readOnly = true)
    public List<PortfolioProjectResponse> listOwnProjects(Authentication authentication) {
        UserAccount designer = requireEligibleDesigner(authentication);
        List<InteriorDesignProject> ownedProjects =
                projects.findByDesigner_IdOrderByCreatedAtDescIdDesc(designer.getId());
        if (ownedProjects.isEmpty()) return List.of();
        List<Long> ids = ownedProjects.stream().map(InteriorDesignProject::getId).toList();
        Map<Long, List<ProjectImageResponse>> byProject = images
                .findByProject_IdInOrderByDisplayOrderAscIdAsc(ids).stream()
                .collect(Collectors.groupingBy(ProjectImage::getProjectId,
                        Collectors.mapping(ProjectImageResponse::from, Collectors.toList())));
        var countsByProject = interestMetrics.countsForProjects(ids);
        var metadataByProject = discovery.metadataForProjects(ids);
        return ownedProjects.stream().map(project -> {
            var counts = countsByProject.get(project.getId());
            return PortfolioProjectResponse.from(project,
                    byProject.getOrDefault(project.getId(), List.of()),
                    counts.viewCount(), counts.favouriteCount(),
                    metadataByProject.getOrDefault(project.getId(), Map.of()));
        }).toList();
    }

    @Transactional
    public PortfolioProjectResponse create(Authentication authentication, CreateProjectRequest request,
            List<MultipartFile> uploads) {
        UserAccount designer = requireEligibleDesigner(authentication);
        List<MultipartFile> supplied = uploads == null ? List.of() : uploads;
        if (supplied.size() > ProjectImageStorage.MAX_IMAGES) {
            throw new IllegalArgumentException("Upload no more than 6 images per project");
        }
        // Validate every image before creating a project or writing any files.
        List<ProjectImageStorage.ValidatedImage> validated = supplied.stream()
                .map(storage::validate).toList();
        var category = request.categoryId()==null?null:categories.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Select an existing project category"));
        if(category==null && categories.count()>0) throw new IllegalArgumentException("Select an existing project category");
        InteriorDesignProject project = projects.saveAndFlush(new InteriorDesignProject(designer,
                category, request.title().trim(), request.description().trim(), request.status()));
        discovery.saveMetadata(project.getId(), request.metadata(),true);
        List<String> createdKeys = new ArrayList<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) createdKeys.forEach(storage::deleteQuietly);
            }
        });
        List<ProjectImage> saved = new ArrayList<>();
        for (int index = 0; index < validated.size(); index++) {
            ProjectImageStorage.StoredImage stored = storage.store(validated.get(index));
            createdKeys.add(stored.key());
            saved.add(images.save(new ProjectImage(project, stored.key(), stored.contentType(),
                    stored.sizeBytes(), index)));
        }
        images.flush();
        return PortfolioProjectResponse.from(project,
                saved.stream().map(ProjectImageResponse::from).toList());
    }

    @Transactional
    public PortfolioProjectResponse update(Authentication authentication, Long projectId,
            UpdateProjectRequest request, List<MultipartFile> uploads) {
        UserAccount designer = requireEligibleDesigner(authentication);
        InteriorDesignProject project = projects.findByIdAndDesigner_Id(projectId, designer.getId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));

        List<ProjectImage> existing = images.findByProject_IdInOrderByDisplayOrderAscIdAsc(
                List.of(projectId));
        List<Long> keepIds = request.keepImageIds();
        Set<Long> requestedIds = new HashSet<>(keepIds);
        if (requestedIds.size() != keepIds.size()
                || existing.stream().filter(image -> requestedIds.contains(image.getId())).count()
                        != requestedIds.size()) {
            throw new IllegalArgumentException("Choose photos belonging to this project");
        }
        List<MultipartFile> supplied = uploads == null ? List.of() : uploads;
        if (keepIds.size() + supplied.size() > ProjectImageStorage.MAX_IMAGES) {
            throw new IllegalArgumentException("Keep and upload no more than 6 photos in total");
        }
        List<ProjectImageStorage.ValidatedImage> validated = supplied.stream()
                .map(storage::validate).toList();
        var category = request.categoryId()==null?null:categories.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Select an existing project category"));

        // Check all inputs and ownership before changing the database or touching stored files.
        List<ProjectImage> removed = existing.stream()
                .filter(image -> !requestedIds.contains(image.getId())).toList();
        List<String> removedKeys = removed.stream().map(ProjectImage::getStorageKey).toList();
        List<String> createdKeys = new ArrayList<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) removedKeys.forEach(storage::deleteQuietly);
                else createdKeys.forEach(storage::deleteQuietly);
            }
        });
        project.updateDetails(category, request.title().trim(), request.description().trim(), request.status());
        if(request.metadata()!=null) discovery.saveMetadata(projectId,request.metadata(),false);
        images.deleteAll(removed);
        images.flush();
        int nextOrder = existing.stream().mapToInt(ProjectImage::getDisplayOrder).max().orElse(-1) + 1;
        List<ProjectImage> current = new ArrayList<>(existing.stream()
                .filter(image -> requestedIds.contains(image.getId())).toList());
        for (ProjectImageStorage.ValidatedImage validatedImage : validated) {
            ProjectImageStorage.StoredImage stored = storage.store(validatedImage);
            createdKeys.add(stored.key());
            current.add(images.save(new ProjectImage(project, stored.key(), stored.contentType(),
                    stored.sizeBytes(), nextOrder++)));
        }
        images.flush();
        current.sort(java.util.Comparator.comparingInt(ProjectImage::getDisplayOrder)
                .thenComparing(ProjectImage::getId));
        return PortfolioProjectResponse.from(project,
                current.stream().map(ProjectImageResponse::from).toList());
    }

    @Transactional
    public void delete(Authentication authentication, Long projectId) {
        UserAccount designer = requireEligibleDesigner(authentication);
        InteriorDesignProject project = projects.findByIdAndDesigner_Id(projectId, designer.getId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));

        List<ProjectImage> existing = images.findByProject_IdInOrderByDisplayOrderAscIdAsc(
                List.of(projectId));
        List<String> imageKeys = existing.stream().map(ProjectImage::getStorageKey).toList();
        // Delete metadata before the project because project_images has a foreign key.
        // Keep image files until PostgreSQL confirms the transaction committed.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                imageKeys.forEach(storage::deleteQuietly);
            }
        });
        images.deleteAll(existing);
        images.flush();
        projects.delete(project);
        projects.flush();
    }

    @Transactional(readOnly = true)
    public ImageContent getOwnImage(Authentication authentication, Long projectId, Long imageId) {
        UserAccount designer = requireEligibleDesigner(authentication);
        ProjectImage image = images.findByIdAndProject_IdAndProject_Designer_Id(
                imageId, projectId, designer.getId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        return new ImageContent(storage.read(image.getStorageKey()), image.getContentType());
    }

    public record ImageContent(byte[] data, String contentType) { }

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
