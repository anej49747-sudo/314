package csit314.renovation.design.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.design.dto.DesignDraftCreateRequest;
import csit314.renovation.design.dto.DesignDraftResponse;
import csit314.renovation.design.dto.DesignDraftSaveRequest;
import csit314.renovation.design.dto.DesignDraftSummaryResponse;
import csit314.renovation.design.dto.DesignElementInput;
import csit314.renovation.design.dto.DesignElementResponse;
import csit314.renovation.design.entity.CustomerDesignDraft;
import csit314.renovation.design.entity.DesignElement;
import csit314.renovation.design.repository.CustomerDesignDraftRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DesignDraftService {
    private static final int MAX_DRAFTS = 30;
    private final UserAccountRepository accounts;
    private final CustomerDesignDraftRepository drafts;

    public DesignDraftService(UserAccountRepository accounts, CustomerDesignDraftRepository drafts) {
        this.accounts = accounts;
        this.drafts = drafts;
    }

    @Transactional(readOnly = true)
    public List<DesignDraftSummaryResponse> list(Authentication auth) {
        return drafts.findByCustomer_IdOrderByUpdatedAtDescIdDesc(customer(auth).getId())
                .stream().map(d -> new DesignDraftSummaryResponse(d.getId(), d.getTitle(),
                        d.getFloorWidthCm(), d.getFloorHeightCm(),
                        (int) d.getElements().stream().filter(e -> e.getKind().equals("ROOM")).count(),
                        d.getUpdatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public DesignDraftResponse get(Authentication auth, Long id) { return details(owned(auth, id)); }

    @Transactional
    public DesignDraftResponse create(Authentication auth, DesignDraftCreateRequest request) {
        if (request == null) throw bad("Draft details are required");
        UserAccount owner = customer(auth);
        if (drafts.countByCustomer_Id(owner.getId()) >= MAX_DRAFTS) throw bad("Maximum 30 drafts per customer");
        String title = title(request.title());
        int width = DesignGeometry.floor(request.floorWidthCm(), "Floor width");
        int height = DesignGeometry.floor(request.floorHeightCm(), "Floor height");
        if (!"BLANK".equals(request.template()) && !"SAMPLE".equals(request.template())) {
            throw bad("Choose a blank or sample layout");
        }
        CustomerDesignDraft draft = new CustomerDesignDraft(owner, title, width, height);
        if ("SAMPLE".equals(request.template())) {
            // Templates are generated on the server and remain editable by the customer.
            List<DesignElementInput> starter = sampleLayout(width, height);
            for (int i = 0; i < starter.size(); i++) {
                draft.addElement(DesignGeometry.checked(starter.get(i), i, width, height));
            }
        }
        return details(drafts.saveAndFlush(draft));
    }

    @Transactional
    public DesignDraftResponse save(Authentication auth, Long id, DesignDraftSaveRequest request) {
        CustomerDesignDraft draft = owned(auth, id);
        if (request == null || request.version() == null) throw bad("Draft version is required");
        if (!draft.getVersion().equals(request.version())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This design was edited in another session. Reload before saving.");
        }
        String title = title(request.title());
        int width = DesignGeometry.floor(request.floorWidthCm(), "Floor width");
        int height = DesignGeometry.floor(request.floorHeightCm(), "Floor height");
        if (request.elements() == null || request.elements().size() > DesignGeometry.MAX_ELEMENTS) {
            throw bad("Up to 120 elements may be saved");
        }
        List<DesignElement> checked = new ArrayList<>();
        for (int i = 0; i < request.elements().size(); i++) {
            checked.add(DesignGeometry.checked(request.elements().get(i), i, width, height));
        }
        draft.replace(title, width, height, checked);
        drafts.saveAndFlush(draft);
        return details(draft);
    }

    @Transactional
    public void delete(Authentication auth, Long id) { drafts.delete(owned(auth, id)); }

    private DesignDraftResponse details(CustomerDesignDraft draft) {
        List<DesignElementResponse> elements = draft.getElements().stream()
                .map(e -> new DesignElementResponse(e.getKind(), e.getName(), e.getXCm(), e.getYCm(),
                        e.getWidthCm(), e.getHeightCm(), e.getRotation())).toList();
        return new DesignDraftResponse(draft.getId(), draft.getTitle(), draft.getFloorWidthCm(),
                draft.getFloorHeightCm(), draft.getVersion(), draft.getUpdatedAt(),
                DesignGeometry.roomOverlaps(draft.getElements()), elements);
    }

    private CustomerDesignDraft owned(Authentication auth, Long id) {
        if (id == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Design draft not found");
        return drafts.findByIdAndCustomer_Id(id, customer(auth).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Design draft not found"));
    }

    private UserAccount customer(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Customer login required");
        return accounts.findByEmailIgnoreCase(auth.getName())
                .filter(account -> account.isActive() && account.getRole() == UserRole.CUSTOMER)
                .orElseThrow(() -> new AccessDeniedException("Active Customer access required"));
    }

    private static String title(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 120) {
            throw bad("Title must contain 1–120 characters");
        }
        return value.trim();
    }

    private static List<DesignElementInput> sampleLayout(int width, int height) {
        // Match the approved two-bedroom prototype. Scale edges (rather than independent
        // widths) so adjoining rooms still share walls at other floor dimensions.
        return List.of(
                scaled("ROOM", "Living room", 0, 0, 600, 450, width, height),
                scaled("ROOM", "Kitchen", 600, 0, 350, 300, width, height),
                scaled("ROOM", "Bathroom", 950, 0, 250, 300, width, height),
                scaled("ROOM", "Bedroom", 600, 300, 600, 500, width, height),
                scaled("ROOM", "Bedroom", 0, 450, 600, 350, width, height),
                scaled("FURNITURE", "Sofa", 80, 160, 230, 95, width, height),
                scaled("FURNITURE", "Table", 310, 250, 150, 90, width, height),
                scaled("FURNITURE", "Bed", 810, 460, 230, 190, width, height),
                scaled("FURNITURE", "Cabinet", 50, 540, 210, 60, width, height),
                scaled("OPENING", "Door", 560, 390, 85, 24, width, height),
                scaled("OPENING", "Window", 165, 0, 160, 22, width, height));
    }

    private static DesignElementInput scaled(String kind, String name, int x, int y, int w, int h,
            int floorWidth, int floorHeight) {
        int left = Math.round(x * floorWidth / 1200f);
        int top = Math.round(y * floorHeight / 800f);
        int right = Math.round((x + w) * floorWidth / 1200f);
        int bottom = Math.round((y + h) * floorHeight / 800f);
        return new DesignElementInput(kind, name, left, top,
                Math.max(20, right - left), Math.max(20, bottom - top), 0);
    }

    private static ResponseStatusException bad(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
