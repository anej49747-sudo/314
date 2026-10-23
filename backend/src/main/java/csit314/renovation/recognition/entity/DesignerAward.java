package csit314.renovation.recognition.entity;

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
@Table(name = "designer_awards")
public class DesignerAward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "designer_id", nullable = false)
    private UserAccount designer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_by")
    private UserAccount awardedBy;

    @Column(nullable = false, length = 70)
    private String title;

    @Column(name = "title_key", nullable = false, length = 160)
    private String titleKey;

    @Column(name = "awarded_at", nullable = false, updatable = false)
    private Instant awardedAt;

    protected DesignerAward() { }

    public DesignerAward(UserAccount designer, UserAccount awardedBy, String title, String titleKey) {
        this.designer = designer;
        this.awardedBy = awardedBy;
        this.title = title;
        this.titleKey = titleKey;
    }

    @PrePersist
    void beforePersist() {
        if (awardedAt == null) awardedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserAccount getDesigner() { return designer; }
    public String getTitle() { return title; }
    public Instant getAwardedAt() { return awardedAt; }
}
