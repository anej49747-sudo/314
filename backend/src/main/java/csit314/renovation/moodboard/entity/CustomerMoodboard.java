package csit314.renovation.moodboard.entity;

import csit314.renovation.account.entity.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_moodboards")
public class CustomerMoodboard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserAccount customer;

    @Column(nullable = false, length = 120)
    private String title;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "background_color", nullable = false, length = 7)
    private String backgroundColor = "#FFFFFF";

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("layerOrder ASC")
    private List<MoodboardElement> elements = new ArrayList<>();

    protected CustomerMoodboard() {}

    public CustomerMoodboard(UserAccount customer, String title) {
        this.customer = customer;
        this.title = title;
    }

    @PrePersist void created() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate void touched() { updatedAt = Instant.now(); }

    public void rename(String newTitle) { title = newTitle; }
    public void changeBackground(String newBackground) { backgroundColor = newBackground; }
    public void replaceElements(List<MoodboardElement> replacements) {
        elements.clear();
        replacements.forEach(this::addElement);
        updatedAt = Instant.now();
    }
    public void addElement(MoodboardElement element) {
        element.attachTo(this);
        elements.add(element);
    }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getBackgroundColor() { return backgroundColor; }
    public Long getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<MoodboardElement> getElements() { return elements; }
}
