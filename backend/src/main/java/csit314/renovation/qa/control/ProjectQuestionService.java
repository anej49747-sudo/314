package csit314.renovation.qa.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.project.control.PublicProjectDetailsService;
import csit314.renovation.qa.dto.DesignerQuestionQueueResponse;
import csit314.renovation.qa.dto.ProjectQuestionPageResponse;
import csit314.renovation.qa.dto.ProjectQuestionResponse;
import csit314.renovation.qa.entity.ProjectQuestion;
import csit314.renovation.qa.repository.ProjectQuestionRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectQuestionService {
    private final ProjectQuestionRepository questions;
    private final UserAccountRepository accounts;
    private final PublicProjectDetailsService projects;

    public ProjectQuestionService(ProjectQuestionRepository questions, UserAccountRepository accounts,
            PublicProjectDetailsService projects) {
        this.questions = questions;
        this.accounts = accounts;
        this.projects = projects;
    }

    @Transactional(readOnly = true)
    public ProjectQuestionPageResponse publicAnswers(Long projectId, String query, String order, int page, int size) {
        projects.requirePublicProject(projectId);
        if (page < 0 || (size != 3 && size != 6)) throw badRequest("Invalid page size");
        if (!"newest".equals(order) && !"oldest".equals(order)) throw badRequest("Invalid sort order");
        String term = search(query);
        Sort sort = Sort.by(order.equals("newest") ? Sort.Direction.DESC : Sort.Direction.ASC,
                "answeredAt", "id");
        Page<ProjectQuestion> entries = questions.publicAnswers(projectId, term, PageRequest.of(page, size, sort));
        return new ProjectQuestionPageResponse(entries.getContent().stream()
                .map(ProjectQuestionResponse::from).toList(), entries.getTotalElements(), page,
                entries.getTotalPages());
    }

    @Transactional
    public ProjectQuestionResponse ask(Authentication authentication, Long projectId, String rawQuestion) {
        UserAccount customer = authenticated(authentication, UserRole.CUSTOMER);
        String question = validateContent(rawQuestion, 10, 600, "Question");
        InteriorDesignProject project = projects.requirePublicProject(projectId);
        ProjectQuestion saved = questions.saveAndFlush(new ProjectQuestion(project, customer, question));
        return ProjectQuestionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ProjectQuestionPageResponse customerPending(Authentication authentication, Long projectId) {
        UserAccount customer = authenticated(authentication, UserRole.CUSTOMER);
        projects.requirePublicProject(projectId);
        Page<ProjectQuestion> result = questions.findByCustomer_IdAndProject_IdAndAnsweredAtIsNull(
                customer.getId(), projectId, PageRequest.of(0, 3,
                        Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return new ProjectQuestionPageResponse(result.getContent().stream()
                .map(ProjectQuestionResponse::from).toList(), result.getTotalElements(), 0,
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public DesignerQuestionQueueResponse designerQueue(Authentication authentication, Long projectId,
            String status, String query, int page) {
        UserAccount designer = authenticated(authentication, UserRole.INTERIOR_DESIGNER);
        if (page < 0) throw badRequest("Invalid page");
        if (!List.of("pending", "answered", "all").contains(status)) throw badRequest("Invalid status");
        if (projectId != null && projectId < 1) throw badRequest("Invalid project");
        String term = search(query);
        Sort sort = "answered".equals(status)
                ? Sort.by(Sort.Direction.DESC, "answeredAt", "id")
                : Sort.by(Sort.Direction.ASC, "createdAt", "id");
        Page<ProjectQuestion> result = questions.designerQueue(designer.getId(), projectId, status,
                term, PageRequest.of(page, 5, sort));
        return new DesignerQuestionQueueResponse(result.getContent().stream()
                .map(ProjectQuestionResponse::from).toList(), result.getTotalElements(), page,
                result.getTotalPages(), questions.countByProject_Designer_IdAndAnsweredAtIsNull(designer.getId()),
                questions.countByProject_Designer_IdAndAnsweredAtIsNotNull(designer.getId()),
                questions.countByProject_Designer_Id(designer.getId()));
    }

    @Transactional
    public ProjectQuestionResponse answer(Authentication authentication, Long questionId, String rawAnswer) {
        UserAccount designer = authenticated(authentication, UserRole.INTERIOR_DESIGNER);
        String content = validateContent(rawAnswer, 1, 2000, "Answer");
        ProjectQuestion question = questions.lockForAnswer(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!question.getProject().getDesigner().getId().equals(designer.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        if (question.getAnswer() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Already answered");
        if (!question.getCustomer().isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question author unavailable");
        }
        question.publishAnswer(content);
        return ProjectQuestionResponse.from(questions.saveAndFlush(question));
    }

    private UserAccount authenticated(Authentication authentication, UserRole role) {
        if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return accounts.findByEmailIgnoreCase(authentication.getName())
                .filter(a -> a.isActive() && a.getRole() == role)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Role access required"));
    }

    private String validateContent(String raw, int min, int max, String label) {
        if (raw == null) throw badRequest(label + " is required");
        String content = raw.strip();
        if (content.length() < min || content.length() > max) {
            throw badRequest(label + " must contain " + min + "–" + max + " characters");
        }
        return content;
    }

    private String search(String query) {
        if (query == null) return "";
        String value = query.strip().toLowerCase(java.util.Locale.ROOT);
        if (value.length() > 100) throw badRequest("Search is too long");
        return value;
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
