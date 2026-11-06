package csit314.renovation.moodboard.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.favourite.entity.CustomerProjectFavourite;
import csit314.renovation.favourite.repository.CustomerProjectFavouriteRepository;
import csit314.renovation.moodboard.dto.MoodboardElementInput;
import csit314.renovation.moodboard.dto.MoodboardElementResponse;
import csit314.renovation.moodboard.dto.MoodboardPhotoResponse;
import csit314.renovation.moodboard.dto.MoodboardResponse;
import csit314.renovation.moodboard.dto.MoodboardSaveRequest;
import csit314.renovation.moodboard.dto.MoodboardSummaryResponse;
import csit314.renovation.moodboard.entity.CustomerMoodboard;
import csit314.renovation.moodboard.entity.MoodboardElement;
import csit314.renovation.moodboard.repository.CustomerMoodboardRepository;
import csit314.renovation.portfolio.entity.ProjectImage;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
import csit314.renovation.project.control.PublicProjectDetailsService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** #28 business rules; 900 × 580 coordinate space shared with the canvas editor. */
@Service
public class MoodboardService {
    private static final int WIDTH = 900;
    private static final int HEIGHT = 580;
    private static final int MAX_ELEMENTS = 60;
    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}");
    private final UserAccountRepository accounts;
    private final CustomerMoodboardRepository boards;
    private final CustomerProjectFavouriteRepository favourites;
    private final ProjectImageRepository images;
    private final PublicProjectDetailsService publicProjects;

    public MoodboardService(UserAccountRepository accounts, CustomerMoodboardRepository boards,
            CustomerProjectFavouriteRepository favourites, ProjectImageRepository images,
            PublicProjectDetailsService publicProjects) {
        this.accounts = accounts;
        this.boards = boards;
        this.favourites = favourites;
        this.images = images;
        this.publicProjects = publicProjects;
    }

    @Transactional(readOnly = true)
    public List<MoodboardSummaryResponse> list(Authentication auth) {
        Long customerId = customer(auth).getId();
        return boards.findByCustomer_IdOrderByUpdatedAtDescIdDesc(customerId).stream()
                .map(board -> new MoodboardSummaryResponse(board.getId(), board.getTitle(),
                        board.getUpdatedAt(), board.getElements().size(), coverUrl(board))).toList();
    }

    @Transactional(readOnly = true)
    public MoodboardResponse get(Authentication auth, Long id) {
        return details(owned(auth, id));
    }

    @Transactional
    public MoodboardResponse create(Authentication auth, String title) {
        UserAccount customer = customer(auth);
        if (boards.countByCustomer_Id(customer.getId()) >= 30) bad("Maximum 30 moodboards per account");
        CustomerMoodboard board = new CustomerMoodboard(customer, validTitle(title));
        return details(boards.saveAndFlush(board));
    }

    @Transactional
    public MoodboardResponse save(Authentication auth, Long id, MoodboardSaveRequest request) {
        CustomerMoodboard board = owned(auth, id);
        if (request == null || request.version() == null) bad("Board version is required");
        if (!board.getVersion().equals(request.version())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This board was changed in another session. Reload before saving.");
        }
        String title = validTitle(request.title());
        String background = validColor(request.backgroundColor());
        List<MoodboardElementInput> requested = request.elements();
        if (requested == null || requested.size() > MAX_ELEMENTS) bad("Up to 60 elements are permitted");
        // Validate all new elements before replacing any existing layout data.
        List<MoodboardElement> replacements = new ArrayList<>();
        Long customerId = customer(auth).getId();
        Set<Long> previouslyPinned = new HashSet<>();
        board.getElements().forEach(e -> { if (e.getPhoto() != null) previouslyPinned.add(e.getPhoto().getId()); });
        for (int i = 0; i < requested.size(); i++) {
            replacements.add(toElement(requested.get(i), i, customerId, previouslyPinned));
        }
        board.rename(title);
        board.changeBackground(background);
        board.replaceElements(replacements);
        // Force version advancement even when only the child collection changed.
        boards.saveAndFlush(board);
        return details(board);
    }

    @Transactional
    public void delete(Authentication auth, Long id) { boards.delete(owned(auth, id)); }

    /** Dedicated favourite-photo picker: does NOT call tracked GET /api/projects/{id}. */
    @Transactional(readOnly = true)
    public List<MoodboardPhotoResponse> photos(Authentication auth) {
        Long customerId = customer(auth).getId();
        List<CustomerProjectFavourite> saved = favourites.findVisibleForCustomer(
                customerId, UserRole.INTERIOR_DESIGNER);
        if (saved.isEmpty()) return List.of();
        List<Long> projectIds = saved.stream().map(f -> f.getProject().getId()).toList();
        var byProject = images.findByProject_IdInOrderByDisplayOrderAscIdAsc(projectIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(ProjectImage::getProjectId));
        return saved.stream().map(f -> {
            var project = f.getProject();
            return new MoodboardPhotoResponse(project.getId(), project.getTitle(),
                    byProject.getOrDefault(project.getId(), List.of()).stream()
                            .map(photo -> new MoodboardPhotoResponse.Photo(photo.getId(),
                                    project.getTitle(), photoUrl(photo))).toList());
        }).toList();
    }

    private MoodboardElement toElement(MoodboardElementInput e, int layer, Long customerId,
            Set<Long> previouslyPinned) {
        if (e == null || e.kind() == null) bad("Element type is required");
        int x = number(e.x(), 0, WIDTH, "Left");
        int y = number(e.y(), 0, HEIGHT, "Top");
        int w = number(e.width(), 55, WIDTH, "Width");
        int h = number(e.height(), 45, HEIGHT, "Height");
        if (x + w > WIDTH || y + h > HEIGHT) bad("Elements must remain inside the canvas");
        String kind = e.kind();
        ProjectImage photo = null;
        String label = e.sourceLabel() == null ? null : e.sourceLabel().trim();
        if (label != null && label.length() > 180) bad("Photo label is too long");
        String text = null;
        String color = null;
        Integer fontSize = null;
        switch (kind) {
            case "PHOTO" -> {
                if (e.photoId() != null) {
                    photo = images.findById(e.photoId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown photo"));
                    // Previously pinned photos remain on the board after an Unsave, or
                    // after the source designer becomes inactive (no image URL is exposed).
                    if (!previouslyPinned.contains(photo.getId())) {
                        var project = publicProjects.requirePublicProject(photo.getProjectId());
                        if (!favourites.existsByCustomer_IdAndProject_Id(customerId, project.getId())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Choose photos from your saved projects");
                        }
                        label = project.getTitle();
                    } else if (label == null || label.isBlank()) {
                        label = "Saved project inspiration";
                    }
                } else if (label == null || label.isBlank()) {
                    bad("An unavailable photo needs its original label");
                }
            }
            case "TEXT" -> {
                text = e.text();
                if (text == null || text.isBlank() || text.length() > 180) bad("Text must contain 1–180 characters");
                color = validColor(e.color());
                fontSize = number(e.fontSize(), 12, 56, "Font size");
            }
            case "SWATCH" -> color = validColor(e.color());
            default -> bad("Unsupported moodboard element type");
        }
        return new MoodboardElement(kind, photo, label, text, color, fontSize, x, y, w, h, layer);
    }

    private MoodboardResponse details(CustomerMoodboard board) {
        List<MoodboardElementResponse> elements = board.getElements().stream().map(e -> {
            ProjectImage photo = e.getPhoto();
            String url = null;
            if (photo != null) {
                try { publicProjects.requirePublicProject(photo.getProjectId()); url = photoUrl(photo); }
                catch (ResponseStatusException invisible) { /* Hide inactive/unpublished sources. */ }
            }
            return new MoodboardElementResponse(e.getKind(), photo == null ? null : photo.getId(),
                    url, "PHOTO".equals(e.getKind()) && url == null, e.getSourceLabel(),
                    e.getTextContent(), e.getColor(), e.getFontSize(),
                    e.getX(), e.getY(), e.getWidth(), e.getHeight());
        }).toList();
        return new MoodboardResponse(board.getId(), board.getTitle(), board.getBackgroundColor(),
                board.getVersion(), board.getUpdatedAt(), elements);
    }

    private String coverUrl(CustomerMoodboard board) {
        for (MoodboardElement e : board.getElements()) {
            if (!"PHOTO".equals(e.getKind()) || e.getPhoto() == null) continue;
            try {
                publicProjects.requirePublicProject(e.getPhoto().getProjectId());
                return photoUrl(e.getPhoto());
            } catch (ResponseStatusException ignored) { /* No public photo for this element. */ }
        }
        return null;
    }

    private static String photoUrl(ProjectImage photo) {
        return "/api/projects/" + photo.getProjectId() + "/images/" + photo.getId();
    }

    private CustomerMoodboard owned(Authentication auth, Long id) {
        if (id == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Moodboard not found");
        return boards.findByIdAndCustomer_Id(id, customer(auth).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Moodboard not found"));
    }

    private UserAccount customer(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Customer sign-in required");
        }
        return accounts.findByEmailIgnoreCase(auth.getName())
                .filter(user -> user.isActive() && user.getRole() == UserRole.CUSTOMER)
                .orElseThrow(() -> new AccessDeniedException("Active Customer access required"));
    }

    private static String validTitle(String title) {
        if (title == null || title.isBlank() || title.trim().length() > 120) bad("Title must contain 1–120 characters");
        return title.trim();
    }
    private static String validColor(String color) {
        if (color == null || !HEX.matcher(color).matches()) bad("Choose a valid six-digit colour");
        return color;
    }
    private static int number(Integer value, int min, int max, String name) {
        if (value == null || value < min || value > max) bad(name + " must be between " + min + " and " + max);
        return value;
    }
    private static void bad(String reason) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason); }
}
