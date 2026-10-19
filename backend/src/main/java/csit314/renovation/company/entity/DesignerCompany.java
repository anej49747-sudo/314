package csit314.renovation.company.entity;

import csit314.renovation.account.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "designer_companies")
public class DesignerCompany {
    @Id
    @Column(name = "designer_id")
    private Long designerId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "designer_id", nullable = false)
    private UserAccount designer;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(length = 300)
    private String address;

    @Column(length = 3000)
    private String description;

    protected DesignerCompany() {
        // JPA
    }

    public DesignerCompany(UserAccount designer) {
        this.designer = designer;
    }

    public void updateDetails(String companyName, String address, String description) {
        this.companyName = companyName;
        this.address = address;
        this.description = description;
    }

    public Long getDesignerId() { return designerId; }
    public String getCompanyName() { return companyName; }
    public String getAddress() { return address; }
    public String getDescription() { return description; }
}
