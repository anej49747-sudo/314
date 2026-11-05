package csit314.renovation.project.boundary;

import csit314.renovation.project.control.PublicProjectSearchService;
import csit314.renovation.project.dto.PublicProjectSearchResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicProjectSearchController {
    private final PublicProjectSearchService service;
    private final csit314.renovation.discovery.control.DiscoveryService discovery;

    public PublicProjectSearchController(PublicProjectSearchService service, csit314.renovation.discovery.control.DiscoveryService discovery) {
        this.service = service;
        this.discovery = discovery;
    }

    @GetMapping("/api/projects")
    public List<PublicProjectSearchResponse> searchProjects(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "propertyTypeId", required = false) Long propertyTypeId,
            @RequestParam(name = "sizeRangeId", required = false) Long sizeRangeId,
            @RequestParam(name = "styleIds", required = false) List<Long> styleIds) {
        var results=service.findProjects(query, categoryId);
        if(propertyTypeId!=null || sizeRangeId!=null || (styleIds!=null && !styleIds.isEmpty())) {
            var allowed=discovery.filteredIds(propertyTypeId,sizeRangeId,styleIds);
            results=results.stream().filter(p->allowed.contains(p.id())).toList();
        }
        return results.stream().map(p->new PublicProjectSearchResponse(p.id(),p.title(),p.description(),p.categoryName(),p.status(),p.designer(),p.coverImageUrl(),discovery.metadata(p.id()))).toList();
    }
}
