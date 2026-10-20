package csit314.renovation.portfolio.boundary;

import csit314.renovation.portfolio.control.PortfolioService;
import csit314.renovation.portfolio.dto.CreateProjectRequest;
import csit314.renovation.portfolio.dto.UpdateProjectRequest;
import jakarta.validation.Valid;
import csit314.renovation.portfolio.dto.PortfolioProjectResponse;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/designer/projects")
public class PortfolioController {
    private final PortfolioService portfolios;

    public PortfolioController(PortfolioService portfolios) {
        this.portfolios = portfolios;
    }

    @GetMapping
    public List<PortfolioProjectResponse> list(Authentication authentication) {
        return portfolios.listOwnProjects(authentication);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioProjectResponse create(@Valid @RequestPart("project") CreateProjectRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {
        return portfolios.create(authentication, request, images);
    }

    @PutMapping(value = "/{projectId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PortfolioProjectResponse update(@PathVariable Long projectId,
            @Valid @RequestPart("project") UpdateProjectRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {
        return portfolios.update(authentication, projectId, request, images);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, Authentication authentication) {
        portfolios.delete(authentication, projectId);
    }

    @GetMapping("/{projectId}/images/{imageId}")
    public ResponseEntity<byte[]> image(@PathVariable Long projectId, @PathVariable Long imageId,
            Authentication authentication) {
        PortfolioService.ImageContent content = portfolios.getOwnImage(authentication, projectId, imageId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(content.contentType())).body(content.data());
    }
}
