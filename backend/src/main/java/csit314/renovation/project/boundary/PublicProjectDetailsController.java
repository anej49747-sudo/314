package csit314.renovation.project.boundary;

import csit314.renovation.metrics.control.ProjectViewTrackingService;
import csit314.renovation.project.control.PublicProjectDetailsService;
import csit314.renovation.project.control.PublicProjectPhotoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import csit314.renovation.project.dto.PublicProjectDetailsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicProjectDetailsController {
    private final PublicProjectDetailsService service;
    private final PublicProjectPhotoService photos;
    private final ProjectViewTrackingService viewTracking;

    public PublicProjectDetailsController(PublicProjectDetailsService service,
            PublicProjectPhotoService photos, ProjectViewTrackingService viewTracking) {
        this.service = service;
        this.photos = photos;
        this.viewTracking = viewTracking;
    }

    @GetMapping("/api/projects/{projectId}")
    public PublicProjectDetailsResponse getProject(@PathVariable("projectId") Long projectId,
            HttpServletRequest request) {
        PublicProjectDetailsResponse details = service.findPublicProject(projectId);
        viewTracking.recordPublicView(projectId, request);
        return details;
    }

    @GetMapping("/api/projects/{projectId}/images/{imageId}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long projectId, @PathVariable Long imageId) {
        PublicProjectPhotoService.ImageContent image = photos.getPublicPhoto(projectId, imageId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(image.contentType())).body(image.data());
    }
}
