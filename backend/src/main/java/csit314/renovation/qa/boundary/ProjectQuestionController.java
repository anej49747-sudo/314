package csit314.renovation.qa.boundary;

import csit314.renovation.qa.control.ProjectQuestionService;
import csit314.renovation.qa.dto.AnswerProjectQuestionRequest;
import csit314.renovation.qa.dto.AskProjectQuestionRequest;
import csit314.renovation.qa.dto.DesignerQuestionQueueResponse;
import csit314.renovation.qa.dto.ProjectQuestionPageResponse;
import csit314.renovation.qa.dto.ProjectQuestionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProjectQuestionController {
    private final ProjectQuestionService service;

    public ProjectQuestionController(ProjectQuestionService service) { this.service = service; }

    @GetMapping("/projects/{projectId}/questions")
    public ProjectQuestionPageResponse answered(@PathVariable Long projectId,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "newest") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size) {
        return service.publicAnswers(projectId, query, order, page, size);
    }

    @PostMapping("/projects/{projectId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectQuestionResponse ask(Authentication authentication, @PathVariable Long projectId,
            @Valid @RequestBody AskProjectQuestionRequest request) {
        return service.ask(authentication, projectId, request.question());
    }

    @GetMapping("/customer/projects/{projectId}/questions")
    public ProjectQuestionPageResponse pending(Authentication authentication, @PathVariable Long projectId) {
        return service.customerPending(authentication, projectId);
    }

    @GetMapping("/designer/project-questions")
    public DesignerQuestionQueueResponse inbox(Authentication authentication,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page) {
        return service.designerQueue(authentication, projectId, status, query, page);
    }

    @PostMapping("/designer/project-questions/{questionId}/answer")
    public ProjectQuestionResponse answer(Authentication authentication, @PathVariable Long questionId,
            @Valid @RequestBody AnswerProjectQuestionRequest request) {
        return service.answer(authentication, questionId, request.answer());
    }
}
