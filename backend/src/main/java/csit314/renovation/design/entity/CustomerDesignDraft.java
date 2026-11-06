package csit314.renovation.design.entity;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_design_drafts")
public class CustomerDesignDraft {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserAccount customer;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "floor_width_cm", nullable = false)
    private int floorWidthCm;

    @Column(name = "floor_height_cm", nullable = false)
    private int floorHeightCm;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "draft", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<DesignElement> elements = new ArrayList<>();

    protected CustomerDesignDraft() {}

    public CustomerDesignDraft(UserAccount customer, String title, int width, int height) {
        this.customer = customer;
        this.title = title;
        this.floorWidthCm = width;
        this.floorHeightCm = height;
    }

    @PrePersist void createTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
    @PreUpdate void touch() { updatedAt = Instant.now(); }

    public void replace(String title, int width, int height, List<DesignElement> newElements) {
        this.title = title;
        floorWidthCm = width;
        floorHeightCm = height;
        elements.clear();
        newElements.forEach(this::addElement);
        updatedAt = Instant.now();
    }
    public void addElement(DesignElement element) {
        element.attach(this);
        elements.add(element);
    }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public int getFloorWidthCm() { return floorWidthCm; }
    public int getFloorHeightCm() { return floorHeightCm; }
    public Long getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<DesignElement> getElements() { return elements; }
}
