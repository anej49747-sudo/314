package csit314.renovation.moodboard.boundary;

import csit314.renovation.moodboard.control.MoodboardService;
import csit314.renovation.moodboard.dto.MoodboardPhotoResponse;
import csit314.renovation.moodboard.dto.MoodboardResponse;
import csit314.renovation.moodboard.dto.MoodboardSaveRequest;
import csit314.renovation.moodboard.dto.MoodboardSummaryResponse;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/customer/moodboards")
public class CustomerMoodboardController {
    private final MoodboardService service;
    public CustomerMoodboardController(MoodboardService service) { this.service = service; }

    @GetMapping public List<MoodboardSummaryResponse> list(Authentication auth) { return service.list(auth); }

    @GetMapping("/photos")
    public List<MoodboardPhotoResponse> photos(Authentication auth) { return service.photos(auth); }

    @GetMapping("/{id}")
    public MoodboardResponse get(Authentication auth, @PathVariable Long id) { return service.get(auth, id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MoodboardResponse create(Authentication auth, @RequestBody Map<String, String> input) {
        return service.create(auth, input.get("title"));
    }

    @PutMapping("/{id}")
    public MoodboardResponse save(Authentication auth, @PathVariable Long id,
            @RequestBody MoodboardSaveRequest input) { return service.save(auth, id, input); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) { service.delete(auth, id); }
}
