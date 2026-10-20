package csit314.renovation.favourite.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.favourite.dto.CustomerFavouriteStatusResponse;
import csit314.renovation.favourite.repository.CustomerProjectFavouriteRepository;
import csit314.renovation.project.control.PublicProjectDetailsService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerFavouriteService {
    private final UserAccountRepository accounts;
    private final CustomerProjectFavouriteRepository favourites;
    private final PublicProjectDetailsService publicProjects;

    public CustomerFavouriteService(UserAccountRepository accounts,
            CustomerProjectFavouriteRepository favourites, PublicProjectDetailsService publicProjects) {
        this.accounts = accounts;
        this.favourites = favourites;
        this.publicProjects = publicProjects;
    }

    @Transactional(readOnly = true)
    public CustomerFavouriteStatusResponse status(Authentication authentication, Long projectId) {
        Long customerId = requireCustomer(authentication).getId();
        publicProjects.requirePublicProject(projectId);
        return new CustomerFavouriteStatusResponse(
                favourites.existsByCustomer_IdAndProject_Id(customerId, projectId));
    }

    @Transactional
    public void save(Authentication authentication, Long projectId) {
        Long customerId = requireCustomer(authentication).getId();
        publicProjects.requirePublicProject(projectId);
        favourites.saveIfAbsent(customerId, projectId);
    }

    @Transactional
    public void remove(Authentication authentication, Long projectId) {
        Long customerId = requireCustomer(authentication).getId();
        // A customer can remove an old favourite even when its designer is now inactive.
        // Neither existence nor visibility is disclosed by this idempotent response.
        favourites.removeIfPresent(customerId, projectId);
    }

    private UserAccount requireCustomer(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Customer sign-in required");
        }
        return accounts.findByEmailIgnoreCase(authentication.getName())
                .filter(account -> account.isActive() && account.getRole() == UserRole.CUSTOMER)
                .orElseThrow(() -> new AccessDeniedException("Active customer access required"));
    }
}
