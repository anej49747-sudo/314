package csit314.renovation.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "account_moderation_events")
public class AccountModerationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private UserAccount account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private UserAccount admin;

    @Column(nullable = false, length = 16)
    private String action;

    @Column(length = 500)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AccountModerationEvent() {}

    public static AccountModerationEvent create(UserAccount account, UserAccount admin,
            String action, String reason) {
        AccountModerationEvent event = new AccountModerationEvent();
        event.account = account;
        event.admin = admin;
        event.action = action;
        event.reason = reason;
        event.occurredAt = Instant.now();
        return event;
    }
}
