package csit314.renovation.report.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActivityReportIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordEncoder passwords;

    private Instant originalTrackingStart;
    private Long customerId;
    private Long designerId;

    @BeforeEach
    void prepare() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        originalTrackingStart = jdbc.queryForObject(
                "SELECT started_at FROM platform_activity_tracking WHERE metric = 'ENGAGEMENT'",
                (rs, n) -> rs.getTimestamp(1).toInstant());
        jdbc.update("DELETE FROM platform_engagement_events");
        accounts.deleteAll();
        String hash = passwords.encode("StrongPassword123!");
        accounts.saveAndFlush(UserAccount.newPlatformManager("Manager", "report-manager@example.com", hash));
        accounts.saveAndFlush(UserAccount.newUserAdmin("Admin", "report-admin@example.com", hash));
        customerId = accounts.saveAndFlush(UserAccount.newCustomer(null,
                "Customer", "report-customer@example.com", hash)).getId();
        designerId = accounts.saveAndFlush(UserAccount.newInteriorDesigner(null,
                "Designer", "report-designer@example.com", hash)).getId();
        jdbc.update("UPDATE platform_activity_tracking SET started_at = ? WHERE metric = 'ENGAGEMENT'",
                Timestamp.from(Instant.parse("2026-10-01T00:00:00Z")));
        jdbc.update("UPDATE user_accounts SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-10-21T19:30:00Z")), customerId);
        jdbc.update("UPDATE user_accounts SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-10-20T12:00:00Z")), designerId);
        jdbc.update("INSERT INTO user_login_events(account_id, successful, occurred_at) VALUES (?, true, ?)",
                customerId, Timestamp.from(Instant.parse("2026-10-21T19:00:00Z")));
        jdbc.update("INSERT INTO user_login_events(account_id, successful, occurred_at) VALUES (?, true, ?)",
                customerId, Timestamp.from(Instant.parse("2026-10-22T02:00:00Z")));
        jdbc.update("INSERT INTO user_login_events(account_id, successful, occurred_at) VALUES (?, false, ?)",
                customerId, Timestamp.from(Instant.parse("2026-10-22T02:05:00Z")));
        jdbc.update("INSERT INTO user_login_events(account_id, successful, occurred_at) VALUES (?, true, ?)",
                designerId, Timestamp.from(Instant.parse("2026-10-22T02:30:00Z")));
        jdbc.update("INSERT INTO platform_engagement_events(event_type, occurred_at) VALUES ('PROJECT_VIEW', ?)",
                Timestamp.from(Instant.parse("2026-10-21T20:00:00Z")));
        jdbc.update("INSERT INTO platform_engagement_events(event_type, occurred_at) VALUES ('FAVOURITE_ADDED', ?)",
                Timestamp.from(Instant.parse("2026-10-22T02:00:00Z")));
        jdbc.update("INSERT INTO platform_engagement_events(event_type, occurred_at) VALUES ('FAVOURITE_REMOVED', ?)",
                Timestamp.from(Instant.parse("2026-10-22T04:00:00Z")));
        jdbc.update("INSERT INTO designer_reviews(designer_id, customer_id, rating, created_at) VALUES (?, ?, 5, ?)",
                designerId, customerId, Timestamp.from(Instant.parse("2026-10-22T04:00:00Z")));
    }

    @AfterEach
    void cleanup() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        jdbc.update("DELETE FROM platform_engagement_events");
        accounts.deleteAll();
        jdbc.update("UPDATE platform_activity_tracking SET started_at = ? WHERE metric = 'ENGAGEMENT'",
                Timestamp.from(originalTrackingStart));
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult csrf = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(csrf.getResponse().getContentAsString());
        MvcResult result = mvc.perform(post("/api/login")
                .session((MockHttpSession) csrf.getRequest().getSession(false))
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email,
                        "password", "StrongPassword123!"))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void managerGetsCorrectSingaporeDayTotalsWithoutFailedOrStaffLogins() throws Exception {
        MockHttpSession manager = login("report-manager@example.com");
        mvc.perform(get("/api/manager/reports/activity").session(manager)
                .param("frequency", "daily").param("date", "2026-10-22"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.timezone").value("Asia/Singapore"))
                .andExpect(jsonPath("$.logins").value(3))
                .andExpect(jsonPath("$.uniqueUsers").value(2))
                .andExpect(jsonPath("$.newAccounts").value(1))
                .andExpect(jsonPath("$.projectViews").value(1))
                .andExpect(jsonPath("$.favouritesAdded").value(1))
                .andExpect(jsonPath("$.favouritesRemoved").value(1))
                .andExpect(jsonPath("$.reviewsSubmitted").value(1))
                .andExpect(jsonPath("$.engagementComplete").value(true))
                .andExpect(jsonPath("$.days.length()").value(1));
    }

    @Test
    void unauthenticatedCustomerAndUserAdminCannotReadReport() throws Exception {
        mvc.perform(get("/api/manager/reports/activity")
                .param("frequency", "weekly").param("date", "2026-10-22"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/manager/reports/activity")
                .session(login("report-customer@example.com"))
                .param("frequency", "weekly").param("date", "2026-10-22"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/manager/reports/activity")
                .session(login("report-admin@example.com"))
                .param("frequency", "weekly").param("date", "2026-10-22"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidPeriodRejectedAndHistoricalEventCoverageNotInvented() throws Exception {
        MockHttpSession manager = login("report-manager@example.com");
        mvc.perform(get("/api/manager/reports/activity").session(manager)
                .param("frequency", "annual").param("date", "2026-10-22"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/manager/reports/activity").session(manager)
                .param("frequency", "monthly").param("date", "2026-09-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.engagementComplete").value(false));
    }
}
