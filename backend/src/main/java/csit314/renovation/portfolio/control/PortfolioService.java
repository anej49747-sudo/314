package csit314.renovation.portfolio.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.portfolio.dto.PortfolioProjectResponse;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {
    private final InteriorDesignProjectRepository projects;
    private final UserAccountRepository accounts;

    public PortfolioService(InteriorDesignProjectRepository projects, UserAccountRepository accounts) {
        this.projects = projects;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<PortfolioProjectResponse> listOwnProjects(Authentication authentication) {
        UserAccount designer = requireEligibleDesigner(authentication);
        return projects.findByDesigner_IdOrderByCreatedAtDescIdDesc(designer.getId())
                .stream().map(PortfolioProjectResponse::from).toList();
    }

    private UserAccount requireEligibleDesigner(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Interior Designer access required");
        }
        return accounts.findByEmailIgnoreCase(authentication.getName())
                .filter(account -> account.isActive() && account.getRole() == UserRole.INTERIOR_DESIGNER
                        && !account.isPasswordChangeRequired())
                .orElseThrow(() -> new AccessDeniedException("Interior Designer access required"));
    }
}
