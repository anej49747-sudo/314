package csit314.renovation.report.control;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.company.repository.DesignerCompanyRepository;
import csit314.renovation.favourite.control.CustomerFavouriteService;
import csit314.renovation.favourite.repository.CustomerProjectFavouriteRepository;
import csit314.renovation.metrics.control.ProjectViewTrackingService;
import csit314.renovation.metrics.repository.ProjectInterestMetricsRepository;
import csit314.renovation.project.control.PublicProjectDetailsService;
import csit314.renovation.report.repository.PlatformEngagementRecorder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;

class EngagementRecordingTest {
    @Test
    void publicProjectViewEventIsEmittedOnlyForFirstViewInSession() {
        ProjectInterestMetricsRepository totals = org.mockito.Mockito.mock(ProjectInterestMetricsRepository.class);
        PlatformEngagementRecorder engagement = org.mockito.Mockito.mock(PlatformEngagementRecorder.class);
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getSession(true)).thenReturn(new MockHttpSession());
        ProjectViewTrackingService tracker = new ProjectViewTrackingService(totals, engagement);
        tracker.recordPublicView(10L, request);
        tracker.recordPublicView(10L, request);
        verify(totals, times(1)).incrementViews(10L);
        verify(engagement, times(1)).recordProjectView();
    }

    @Test
    void repeatedSaveAndRemovalDoNotCreatePhantomEvents() {
        UserAccountRepository accounts = org.mockito.Mockito.mock(UserAccountRepository.class);
        CustomerProjectFavouriteRepository favourites = org.mockito.Mockito.mock(CustomerProjectFavouriteRepository.class);
        PublicProjectDetailsService projects = org.mockito.Mockito.mock(PublicProjectDetailsService.class);
        DesignerCompanyRepository companies = org.mockito.Mockito.mock(DesignerCompanyRepository.class);
        PlatformEngagementRecorder engagement = org.mockito.Mockito.mock(PlatformEngagementRecorder.class);
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        UserAccount customer = org.mockito.Mockito.mock(UserAccount.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("customer@example.com");
        when(customer.isActive()).thenReturn(true);
        when(customer.getRole()).thenReturn(UserRole.CUSTOMER);
        when(customer.getId()).thenReturn(5L);
        when(accounts.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));
        when(favourites.saveIfAbsent(5L, 8L)).thenReturn(1, 0);
        when(favourites.removeIfPresent(5L, 8L)).thenReturn(1, 0);
        CustomerFavouriteService service = new CustomerFavouriteService(accounts, favourites,
                projects, companies, org.mockito.Mockito.mock(csit314.renovation.portfolio.repository.ProjectImageRepository.class), engagement);
        service.save(auth, 8L);
        service.save(auth, 8L);
        service.remove(auth, 8L);
        service.remove(auth, 8L);
        verify(engagement, times(1)).recordFavouriteAdded();
        verify(engagement, times(1)).recordFavouriteRemoved();
    }
}
