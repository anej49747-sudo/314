package csit314.renovation.messaging.entity;

import csit314.renovation.account.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "private_conversations")
public class PrivateConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserAccount customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "designer_id", nullable = false)
    private UserAccount designer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    protected PrivateConversation() { }

    public PrivateConversation(UserAccount customer, UserAccount designer) {
        this.customer = customer;
        this.designer = designer;
    }

    @PrePersist
    void initTimestamps() {
        if (createdAt == null) createdAt = Instant.now();
        if (lastMessageAt == null) lastMessageAt = createdAt;
    }

    public void recordMessage(Instant timestamp) { lastMessageAt = timestamp; }
    public Long getId() { return id; }
    public UserAccount getCustomer() { return customer; }
    public UserAccount getDesigner() { return designer; }
    public Instant getLastMessageAt() { return lastMessageAt; }
}
