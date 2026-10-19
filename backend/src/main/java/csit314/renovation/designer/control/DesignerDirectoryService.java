package csit314.renovation.designer.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.company.entity.DesignerCompany;
import csit314.renovation.company.repository.DesignerCompanyRepository;
import csit314.renovation.designer.dto.DesignerDirectoryResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DesignerDirectoryService {
    private final UserAccountRepository accounts;
    private final DesignerCompanyRepository companies;

    public DesignerDirectoryService(UserAccountRepository accounts, DesignerCompanyRepository companies) {
        this.accounts = accounts;
        this.companies = companies;
    }

    @Transactional(readOnly = true)
    public List<DesignerDirectoryResponse> listDesigners() {
        // Designer accounts are provisioned by User Admin (#37); there is no separate approval flag.
        List<UserAccount> designers = accounts.findByRoleAndActiveTrue(UserRole.INTERIOR_DESIGNER);
        if (designers.isEmpty()) return List.of();

        Map<Long, DesignerCompany> byDesignerId = companies.findAllById(
                designers.stream().map(UserAccount::getId).toList()).stream()
                .collect(Collectors.toMap(DesignerCompany::getDesignerId, Function.identity()));

        return designers.stream().map(designer -> {
            DesignerCompany company = byDesignerId.get(designer.getId());
            String name = company == null || company.getCompanyName() == null
                    || company.getCompanyName().isBlank()
                    ? designer.getDisplayName() : company.getCompanyName();
            return new DesignerDirectoryResponse(designer.getId(), name,
                    company == null ? null : company.getAddress(),
                    company == null ? null : company.getDescription());
        }).sorted(Comparator.comparing(DesignerDirectoryResponse::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(DesignerDirectoryResponse::id)).toList();
    }
}
