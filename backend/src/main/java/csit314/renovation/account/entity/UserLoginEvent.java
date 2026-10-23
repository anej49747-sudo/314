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
@Table(name = "user_login_events")
public class UserLoginEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private UserAccount account;

    @Column(nullable = false)
    private boolean successful;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected UserLoginEvent() {}

    public static UserLoginEvent attempt(UserAccount account, boolean successful) {
        UserLoginEvent event = new UserLoginEvent();
        event.account = account;
        event.successful = successful;
        event.occurredAt = Instant.now();
        return event;
    }

    public Long getId() { return id; }
    public boolean isSuccessful() { return successful; }
    public Instant getOccurredAt() { return occurredAt; }
}
