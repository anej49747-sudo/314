package csit314.renovation.favourite.entity;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "customer_project_favourites", uniqueConstraints = @UniqueConstraint(
        name = "customer_project_favourites_customer_project_uq", columnNames = {"customer_id", "project_id"}))
public class CustomerProjectFavourite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserAccount customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private InteriorDesignProject project;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected CustomerProjectFavourite() {
        // JPA
    }

    public Long getId() { return id; }
    public UserAccount getCustomer() { return customer; }
    public InteriorDesignProject getProject() { return project; }
    public Instant getCreatedAt() { return createdAt; }
}
