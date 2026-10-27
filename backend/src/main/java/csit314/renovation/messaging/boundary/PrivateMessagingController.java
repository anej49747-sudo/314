package csit314.renovation.messaging.boundary;

import csit314.renovation.messaging.control.PrivateMessagingService;
import csit314.renovation.messaging.dto.PrivateConversationResponse;
import csit314.renovation.messaging.dto.PrivateMessagePageResponse;
import csit314.renovation.messaging.dto.PrivateMessageResponse;
import csit314.renovation.messaging.dto.SendPrivateMessageRequest;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/messages")
public class PrivateMessagingController {
    private final PrivateMessagingService service;

    public PrivateMessagingController(PrivateMessagingService service) { this.service = service; }

    @GetMapping("/conversations")
    public List<PrivateConversationResponse> list(Authentication authentication) {
        return service.list(authentication);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public PrivateMessagePageResponse history(Authentication authentication,
            @PathVariable Long conversationId, @RequestParam(required = false) Long before) {
        return service.history(authentication, conversationId, before);
    }

    @PostMapping("/designers/{designerId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public PrivateMessageResponse start(Authentication authentication, @PathVariable Long designerId,
            @Valid @RequestBody SendPrivateMessageRequest request) {
        return service.startOrSend(authentication, designerId, request.body());
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public PrivateMessageResponse reply(Authentication authentication, @PathVariable Long conversationId,
            @Valid @RequestBody SendPrivateMessageRequest request) {
        return service.reply(authentication, conversationId, request.body());
    }
}
