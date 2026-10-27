package csit314.renovation.messaging.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.company.repository.DesignerCompanyRepository;
import csit314.renovation.messaging.dto.PrivateConversationResponse;
import csit314.renovation.messaging.dto.PrivateMessagePageResponse;
import csit314.renovation.messaging.dto.PrivateMessageResponse;
import csit314.renovation.messaging.entity.PrivateConversation;
import csit314.renovation.messaging.entity.PrivateMessage;
import csit314.renovation.messaging.repository.PrivateConversationRepository;
import csit314.renovation.messaging.repository.PrivateMessageRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PrivateMessagingService {
    private static final int MESSAGE_PAGE_SIZE = 50;
    private final UserAccountRepository accounts;
    private final DesignerCompanyRepository companies;
    private final PrivateConversationRepository conversations;
    private final PrivateMessageRepository messages;

    public PrivateMessagingService(UserAccountRepository accounts, DesignerCompanyRepository companies,
            PrivateConversationRepository conversations, PrivateMessageRepository messages) {
        this.accounts = accounts;
        this.companies = companies;
        this.conversations = conversations;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public List<PrivateConversationResponse> list(Authentication authentication) {
        UserAccount viewer = currentParticipant(authentication);
        List<PrivateConversation> threads = viewer.getRole() == UserRole.CUSTOMER
                ? conversations.findByCustomer_IdOrderByLastMessageAtDescIdDesc(viewer.getId())
                : conversations.findByDesigner_IdOrderByLastMessageAtDescIdDesc(viewer.getId());
        return threads.stream().map(thread -> {
            UserAccount counterpart = viewer.getRole() == UserRole.CUSTOMER
                    ? thread.getDesigner() : thread.getCustomer();
            String displayName = counterpart.getDisplayName();
            if (viewer.getRole() == UserRole.CUSTOMER) {
                displayName = companies.findById(counterpart.getId())
                        .map(company -> company.getCompanyName() == null || company.getCompanyName().isBlank()
                                ? counterpart.getDisplayName() : company.getCompanyName())
                        .orElse(displayName);
            }
            List<PrivateMessage> latest = messages.findByConversation_IdOrderByIdDesc(thread.getId(),
                    PageRequest.of(0, 1));
            return new PrivateConversationResponse(thread.getId(), counterpart.getId(), displayName,
                    latest.isEmpty() ? "" : latest.getFirst().getBody(), thread.getLastMessageAt());
        }).toList();
    }

    @Transactional(readOnly = true)
    public PrivateMessagePageResponse history(Authentication authentication, Long conversationId, Long before) {
        UserAccount viewer = currentParticipant(authentication);
        PrivateConversation thread = requireParticipant(conversationId, viewer);
        if (before != null && before <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid message cursor");
        }
        List<PrivateMessage> descending = before == null
                ? messages.findByConversation_IdOrderByIdDesc(thread.getId(), PageRequest.of(0, MESSAGE_PAGE_SIZE + 1))
                : messages.findByConversation_IdAndIdLessThanOrderByIdDesc(
                        thread.getId(), before, PageRequest.of(0, MESSAGE_PAGE_SIZE + 1));
        boolean hasMore = descending.size() > MESSAGE_PAGE_SIZE;
        List<PrivateMessageResponse> chronological = new ArrayList<>(descending.subList(
                0, Math.min(descending.size(), MESSAGE_PAGE_SIZE)).stream().map(PrivateMessageResponse::from).toList());
        Collections.reverse(chronological);
        return new PrivateMessagePageResponse(chronological, hasMore);
    }

    @Transactional
    public PrivateMessageResponse startOrSend(Authentication authentication, Long designerId, String body) {
        UserAccount customer = currentParticipant(authentication);
        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer access required");
        }
        String content = validateBody(body);
        UserAccount designer = accounts.findByIdAndRoleAndActiveTrue(designerId, UserRole.INTERIOR_DESIGNER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found"));
        // A row lock on this customer serializes concurrent first sends for the same pair.
        accounts.lockForMessaging(customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        PrivateConversation thread = conversations.findByCustomer_IdAndDesigner_Id(customer.getId(), designerId)
                .orElseGet(() -> conversations.saveAndFlush(new PrivateConversation(customer, designer)));
        return saveMessage(thread, customer, content);
    }

    @Transactional
    public PrivateMessageResponse reply(Authentication authentication, Long conversationId, String body) {
        UserAccount sender = currentParticipant(authentication);
        PrivateConversation thread = requireParticipant(conversationId, sender);
        UserAccount recipient = sender.getRole() == UserRole.CUSTOMER ? thread.getDesigner() : thread.getCustomer();
        if (!recipient.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Recipient is unavailable");
        }
        return saveMessage(thread, sender, validateBody(body));
    }

    private PrivateMessageResponse saveMessage(PrivateConversation thread, UserAccount sender, String body) {
        PrivateMessage saved = messages.saveAndFlush(new PrivateMessage(thread, sender, body));
        thread.recordMessage(saved.getCreatedAt());
        conversations.save(thread);
        return PrivateMessageResponse.from(saved);
    }

    private String validateBody(String body) {
        if (body == null || body.isBlank() || body.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must contain 1–2000 characters");
        }
        String value = body.strip();
        if (value.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        return value;
    }

    private PrivateConversation requireParticipant(Long id, UserAccount viewer) {
        PrivateConversation thread = conversations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        boolean member = viewer.getRole() == UserRole.CUSTOMER
                ? thread.getCustomer().getId().equals(viewer.getId())
                : thread.getDesigner().getId().equals(viewer.getId());
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
        return thread;
    }

    private UserAccount currentParticipant(Authentication authentication) {
        if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return accounts.findByEmailIgnoreCase(authentication.getName())
                .filter(a -> a.isActive() && (a.getRole() == UserRole.CUSTOMER
                        || a.getRole() == UserRole.INTERIOR_DESIGNER))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Messaging access required"));
    }
}
