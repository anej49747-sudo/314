package csit314.renovation.recognition.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
import csit314.renovation.recognition.dto.AwardResponse;
import csit314.renovation.recognition.dto.FeaturedProjectResponse;
import csit314.renovation.recognition.dto.MonthlyFeatureResponse;
import csit314.renovation.recognition.dto.PublicRecognitionResponse;
import csit314.renovation.recognition.entity.DesignerAward;
import csit314.renovation.recognition.entity.ProjectOfMonth;
import csit314.renovation.recognition.repository.DesignerAwardRepository;
import csit314.renovation.recognition.repository.ProjectOfMonthRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecognitionService {
    // The reporting interface uses Singapore calendar periods as well.
    private static final ZoneId PLATFORM_ZONE = ZoneId.of("Asia/Singapore");
    private final AuthenticationService authenticationService;
    private final InteriorDesignProjectRepository projects;
    private final ProjectImageRepository projectImages;
    private final UserAccountRepository accounts;
    private final ProjectOfMonthRepository featuredProjects;
    private final DesignerAwardRepository awards;

    public RecognitionService(AuthenticationService authenticationService,
            InteriorDesignProjectRepository projects, ProjectImageRepository projectImages, UserAccountRepository accounts,
            ProjectOfMonthRepository featuredProjects, DesignerAwardRepository awards) {
        this.authenticationService = authenticationService;
        this.projects = projects;
        this.projectImages = projectImages;
        this.accounts = accounts;
        this.featuredProjects = featuredProjects;
        this.awards = awards;
    }

    private void requireManager(Authentication authentication) {
        authenticationService.currentManager(authentication);
    }

    private String validateMonth(String input) {
        if (input == null || !input.matches("[0-9]{4}-(0[1-9]|1[0-2])")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a valid YYYY-MM month");
        }
        try {
            YearMonth.parse(input);
        } catch (DateTimeParseException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a valid YYYY-MM month");
        }
        return input;
    }

    private boolean publiclyVisible(InteriorDesignProject project) {
        return project.getDesigner().getRole() == UserRole.INTERIOR_DESIGNER
                && project.getDesigner().isActive();
    }

    @Transactional(readOnly = true)
    public PublicRecognitionResponse publicRecognition() {
        String currentMonth = YearMonth.from(LocalDate.now(PLATFORM_ZONE)).toString();
        FeaturedProjectResponse featured = featuredProjects.findById(currentMonth)
                .filter(item -> publiclyVisible(item.getProject()))
                .map(item -> {
                    FeaturedProjectResponse response = FeaturedProjectResponse.from(item);
                    var photos = projectImages.findByProject_IdInOrderByDisplayOrderAscIdAsc(
                            List.of(item.getProject().getId()));
                    if (photos.isEmpty()) return response;
                    var first = photos.get(0);
                    return response.withCover("/api/projects/" + item.getProject().getId()
                            + "/images/" + first.getId());
                }).orElse(null);
        return new PublicRecognitionResponse(featured,
                awards.findByDesigner_RoleAndDesigner_ActiveTrueOrderByAwardedAtDescIdDesc(
                        UserRole.INTERIOR_DESIGNER).stream().map(AwardResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public MonthlyFeatureResponse monthlyFeature(String month, Authentication authentication) {
        requireManager(authentication);
        String period = validateMonth(month);
        return new MonthlyFeatureResponse(period,
                featuredProjects.findById(period).map(FeaturedProjectResponse::from).orElse(null));
    }

    @Transactional
    public MonthlyFeatureResponse selectProject(String month, Long projectId, Authentication authentication) {
        requireManager(authentication);
        String period = validateMonth(month);
        InteriorDesignProject project = projects.findById(projectId)
                .filter(this::publiclyVisible)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Eligible project not found"));
        ProjectOfMonth current = featuredProjects.findById(period)
                .orElseGet(() -> new ProjectOfMonth(period, project));
        current.replace(project);
        ProjectOfMonth saved = featuredProjects.saveAndFlush(current);
        return new MonthlyFeatureResponse(period, FeaturedProjectResponse.from(saved));
    }

    @Transactional
    public void removeFeature(String month, Authentication authentication) {
        requireManager(authentication);
        String period = validateMonth(month);
        if (!featuredProjects.existsById(period)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No project selected for this month");
        }
        featuredProjects.deleteById(period);
    }

    @Transactional(readOnly = true)
    public List<AwardResponse> managerAwards(Authentication authentication) {
        requireManager(authentication);
        return awards.findByDesigner_RoleAndDesigner_ActiveTrueOrderByAwardedAtDescIdDesc(
                UserRole.INTERIOR_DESIGNER).stream().map(AwardResponse::from).toList();
    }

    @Transactional
    public AwardResponse grantAward(Long designerId, String submittedTitle, Authentication authentication) {
        requireManager(authentication);
        String title = submittedTitle == null ? "" : submittedTitle.strip().replaceAll("\\s+", " ");
        if (title.length() < 3 || title.length() > 70) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Award title must be 3–70 characters");
        }
        UserAccount designer = accounts.findByIdAndRoleAndActiveTrue(designerId, UserRole.INTERIOR_DESIGNER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Active designer not found"));
        String titleKey = title.toLowerCase(Locale.ROOT);
        if (awards.existsByDesigner_IdAndTitleKey(designerId, titleKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Designer already has this award title");
        }
        UserAccount manager = accounts.getReferenceById(authenticationService.currentManager(authentication).id());
        return AwardResponse.from(awards.saveAndFlush(new DesignerAward(designer, manager, title, titleKey)));
    }

    @Transactional
    public void removeAward(Long awardId, Authentication authentication) {
        requireManager(authentication);
        if (!awards.existsById(awardId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Award not found");
        }
        awards.deleteById(awardId);
    }
}
