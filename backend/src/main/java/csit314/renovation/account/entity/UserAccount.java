package csit314.renovation.account.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private Salutation salutation;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserAccount() {
        // JPA
    }

    public static UserAccount newCustomer(Salutation salutation, String name, String email, String passwordHash) {
        UserAccount account = new UserAccount();
        account.salutation = salutation;
        account.name = name;
        account.email = email;
        account.passwordHash = passwordHash;
        account.role = UserRole.CUSTOMER;
        account.active = true;
        return account;
    }

    public static UserAccount newInteriorDesigner(Salutation salutation, String name, String email, String passwordHash) {
        UserAccount account = new UserAccount();
        account.salutation = salutation;
        account.name = name;
        account.email = email;
        account.passwordHash = passwordHash;
        account.role = UserRole.INTERIOR_DESIGNER;
        account.passwordChangeRequired = true;
        account.active = true;
        return account;
    }

    public static UserAccount newUserAdmin(String name, String email, String passwordHash) {
        UserAccount account = new UserAccount();
        account.name = name;
        account.email = email;
        account.passwordHash = passwordHash;
        account.role = UserRole.USER_ADMIN;
        account.active = true;
        return account;
    }

    @PrePersist
    void setCreationTime() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Salutation getSalutation() { return salutation; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }
    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }

    public void replacePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordChangeRequired = false;
    }
    public Instant getCreatedAt() { return createdAt; }

    public String getDisplayName() {
        return salutation == null ? name : salutation.label() + " " + name;
    }
}
