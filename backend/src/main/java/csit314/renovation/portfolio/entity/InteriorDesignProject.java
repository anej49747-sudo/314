package csit314.renovation.portfolio.entity;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.category.entity.ProjectCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "interior_design_projects")
public class InteriorDesignProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "designer_id", nullable = false)
    private UserAccount designer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProjectCategory category;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 3000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProjectStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InteriorDesignProject() {
        // JPA
    }

    public InteriorDesignProject(UserAccount designer, ProjectCategory category, String title,
            String description, ProjectStatus status) {
        this.designer = designer;
        this.category = category;
        this.title = title;
        this.description = description;
        this.status = status;
    }

    @PrePersist
    void setCreationTime() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    public Long getId() { return id; }
    public UserAccount getDesigner() { return designer; }
    public ProjectCategory getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ProjectStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
