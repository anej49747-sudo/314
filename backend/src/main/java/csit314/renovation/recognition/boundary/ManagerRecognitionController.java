package csit314.renovation.recognition.boundary;

import csit314.renovation.recognition.control.RecognitionService;
import csit314.renovation.recognition.dto.AwardResponse;
import csit314.renovation.recognition.dto.FeatureProjectRequest;
import csit314.renovation.recognition.dto.GrantAwardRequest;
import csit314.renovation.recognition.dto.MonthlyFeatureResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/recognitions")
public class ManagerRecognitionController {
    private final RecognitionService service;

    public ManagerRecognitionController(RecognitionService service) { this.service = service; }

    @GetMapping("/projects/{month}")
    public MonthlyFeatureResponse month(@PathVariable String month, Authentication authentication) {
        return service.monthlyFeature(month, authentication);
    }

    @PutMapping("/projects/{month}")
    public MonthlyFeatureResponse select(@PathVariable String month,
            @Valid @RequestBody FeatureProjectRequest request, Authentication authentication) {
        return service.selectProject(month, request.projectId(), authentication);
    }

    @DeleteMapping("/projects/{month}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@PathVariable String month, Authentication authentication) {
        service.removeFeature(month, authentication);
    }

    @GetMapping("/awards")
    public List<AwardResponse> awards(Authentication authentication) {
        return service.managerAwards(authentication);
    }

    @PostMapping("/awards")
    @ResponseStatus(HttpStatus.CREATED)
    public AwardResponse award(@Valid @RequestBody GrantAwardRequest request, Authentication authentication) {
        return service.grantAward(request.designerId(), request.title(), authentication);
    }

    @DeleteMapping("/awards/{awardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long awardId, Authentication authentication) {
        service.removeAward(awardId, authentication);
    }
}
