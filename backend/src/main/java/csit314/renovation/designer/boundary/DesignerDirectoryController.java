package csit314.renovation.designer.boundary;

import csit314.renovation.designer.control.DesignerDirectoryService;
import csit314.renovation.designer.dto.DesignerDirectoryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DesignerDirectoryController {
    private final DesignerDirectoryService service;

    public DesignerDirectoryController(DesignerDirectoryService service) {
        this.service = service;
    }

    @GetMapping("/api/designers")
    public List<DesignerDirectoryResponse> listDesigners() {
        return service.listDesigners();
    }
}
