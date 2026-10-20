package csit314.renovation.project.control;

import csit314.renovation.portfolio.control.ProjectImageStorage;
import csit314.renovation.portfolio.entity.ProjectImage;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicProjectPhotoService {
    private final PublicProjectDetailsService details;
    private final ProjectImageRepository images;
    private final ProjectImageStorage storage;

    public PublicProjectPhotoService(PublicProjectDetailsService details,
            ProjectImageRepository images, ProjectImageStorage storage) {
        this.details = details;
        this.images = images;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public ImageContent getPublicPhoto(Long projectId, Long imageId) {
        // Check the parent first, including the designer's current account status.
        details.requirePublicProject(projectId);
        ProjectImage image = images.findByIdAndProject_Id(imageId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project photo not found"));
        return new ImageContent(storage.read(image.getStorageKey()), image.getContentType());
    }

    public record ImageContent(byte[] data, String contentType) { }
}
