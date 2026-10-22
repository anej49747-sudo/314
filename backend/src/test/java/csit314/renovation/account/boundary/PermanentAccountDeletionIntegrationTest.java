package csit314.renovation.account.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.portfolio.control.ProjectImageStorage;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PermanentAccountDeletionIntegrationTest {
    private static final Path TEMP_IMAGES = makeTempDirectory();
    private static final String PASSWORD = "StrongPassword123!";
    private static final Instant REPORT_TIME = Instant.parse("2026-10-21T20:30:00Z");

    @DynamicPropertySource
    static void testImages(DynamicPropertyRegistry registry) {
        registry.add("app.project-images.directory", TEMP_IMAGES::toString);
    }

    private static Path makeTempDirectory() {
        try { return Files.createTempDirectory("csit314-deletion-test-"); }
        catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired ProjectImageStorage storage;

    private Long customerId;
    private Long designerId;
    private Long secondDesignerId;
    private Long adminId;
    private Long projectId;
    private String imageKey;

    @BeforeEach
    void prepare() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        accounts.deleteAll();
        jdbc.update("DELETE FROM project_categories");
        jdbc.update("DELETE FROM deleted_account_activity_totals");
        String hash = passwords.encode(PASSWORD);
        adminId = accounts.saveAndFlush(UserAccount.newUserAdmin(
                "Admin", "delete-admin@example.com", hash)).getId();
        accounts.saveAndFlush(UserAccount.newPlatformManager(
                "Manager", "delete-manager@example.com", hash));
        customerId = accounts.saveAndFlush(UserAccount.newCustomer(null,
                "Customer", "delete-customer@example.com", hash)).getId();
        designerId = accounts.saveAndFlush(UserAccount.newInteriorDesigner(null,
                "Designer", "delete-designer@example.com", hash)).getId();
        secondDesignerId = accounts.saveAndFlush(UserAccount.newInteriorDesigner(null,
                "Other Designer", "delete-other@example.com", hash)).getId();
        jdbc.update("UPDATE user_accounts SET created_at = ? WHERE id IN (?, ?)",
                Timestamp.from(REPORT_TIME), customerId, designerId);
        jdbc.update("INSERT INTO user_login_events(account_id, successful, occurred_at) "
                + "VALUES (?, true, ?), (?, true, ?), (?, true, ?)",
                customerId, Timestamp.from(REPORT_TIME), customerId, Timestamp.from(REPORT_TIME),
                designerId, Timestamp.from(REPORT_TIME));
        jdbc.update("INSERT INTO designer_reviews(designer_id, customer_id, rating, created_at) "
                + "VALUES (?, ?, 5, ?)", designerId, customerId, Timestamp.from(REPORT_TIME));
        Long categoryId = jdbc.queryForObject("INSERT INTO project_categories(name) "
                + "VALUES ('Delete tests') RETURNING id", Long.class);
        projectId = jdbc.queryForObject("INSERT INTO interior_design_projects "
                + "(designer_id, category_id, title, description, status) "
                + "VALUES (?, ?, 'Demo', 'For deletion test', 'CURRENT') RETURNING id",
                Long.class, designerId, categoryId);
        jdbc.update("INSERT INTO customer_project_favourites(customer_id, project_id) VALUES (?, ?)",
                customerId, projectId);
        jdbc.update("INSERT INTO designer_reports(customer_id, designer_id, reason, description) "
                + "VALUES (?, ?, 'OTHER', 'Example complaint with sufficient detail')",
                customerId, designerId);
        var stored = storage.store(new ProjectImageStorage.ValidatedImage(
                new byte[] {1, 2, 3, 4}, "image/png", ".png"));
        imageKey = stored.key();
        jdbc.update("INSERT INTO project_images(project_id, storage_key, content_type, size_bytes, display_order) "
                + "VALUES (?, ?, 'image/png', 4, 0)", projectId, imageKey);
    }

    @AfterEach
    void cleanup() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        accounts.deleteAll();
        jdbc.update("DELETE FROM project_categories");
        jdbc.update("DELETE FROM deleted_account_activity_totals");
        storage.deleteQuietly(imageKey);
    }

    private record Session(MockHttpSession session, String header, String token) {}

    private Session login(String email) throws Exception {
        MvcResult first = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode csrf = json.readTree(first.getResponse().getContentAsString());
        MvcResult signedIn = mvc.perform(post("/api/login")
                .session((MockHttpSession) first.getRequest().getSession(false))
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) signedIn.getRequest().getSession(false);
        MvcResult tokenResult = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(tokenResult.getResponse().getContentAsString());
        return new Session(session, token.get("headerName").asText(), token.get("token").asText());
    }

    private JsonNode report(Session manager, String frequency) throws Exception {
        MvcResult result = mvc.perform(get("/api/manager/reports/activity")
                .session(manager.session()).param("frequency", frequency).param("date", "2026-10-22"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private void assertSameMetrics(JsonNode before, JsonNode after) {
        for (String key : new String[] {"logins", "uniqueUsers", "newAccounts", "projectViews",
                "favouritesAdded", "favouritesRemoved", "reviewsSubmitted"}) {
            assertEquals(before.get(key).asLong(), after.get(key).asLong(), key);
        }
    }

    @Test
    void adminDeletionCascadesDesignerDataAndImagesButKeepsAnonymousReports() throws Exception {
        Session manager = login("delete-manager@example.com");
        Map<String, JsonNode> before = Map.of("daily", report(manager, "daily"),
                "weekly", report(manager, "weekly"), "monthly", report(manager, "monthly"));
        Session admin = login("delete-admin@example.com");
        String uri = "/api/admin/accounts/" + designerId;
        mvc.perform(delete(uri).session(admin.session())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/accounts/" + customerId)
                .session(admin.session()).header(admin.header(), admin.token()))
                .andExpect(status().isNotFound());
        mvc.perform(delete(uri).session(admin.session()).header(admin.header(), admin.token()))
                .andExpect(status().isNoContent());
        assertFalse(accounts.existsById(designerId));
        assertTrue(accounts.existsById(customerId));
        assertTrue(accounts.existsById(secondDesignerId));
        assertEquals(0L, count("SELECT COUNT(*) FROM interior_design_projects WHERE id = ?", projectId));
        assertEquals(0L, count("SELECT COUNT(*) FROM project_images WHERE storage_key = ?", imageKey));
        assertEquals(0L, count("SELECT COUNT(*) FROM designer_reports WHERE designer_id = ?", designerId));
        assertEquals(0L, count("SELECT COUNT(*) FROM designer_reviews WHERE designer_id = ?", designerId));
        assertThrows(java.io.UncheckedIOException.class, () -> storage.read(imageKey));
        for (String frequency : before.keySet()) {
            assertSameMetrics(before.get(frequency), report(manager, frequency));
        }
        mvc.perform(delete(uri).session(admin.session()).header(admin.header(), admin.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerSelfDeletionRevokesSessionAndPreservesReports() throws Exception {
        Session manager = login("delete-manager@example.com");
        Session customer = login("delete-customer@example.com");
        JsonNode before = report(manager, "weekly");
        Session admin = login("delete-admin@example.com");
        mvc.perform(delete("/api/customer/account").session(admin.session())
                .header(admin.header(), admin.token())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/customer/account").session(customer.session()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/customer/account").session(customer.session())
                .header(customer.header(), customer.token()))
                .andExpect(status().isNoContent());
        assertFalse(accounts.existsById(customerId));
        assertTrue(accounts.existsById(designerId));
        assertTrue(customer.session().isInvalid());
        assertEquals(0L, count("SELECT COUNT(*) FROM customer_project_favourites WHERE customer_id = ?",
                customerId));
        assertEquals(0L, count("SELECT COUNT(*) FROM designer_reports WHERE customer_id = ?", customerId));
        assertSameMetrics(before, report(manager, "weekly"));
        // The invalidated session cannot be reused as an authenticated session.
    }

    @Test
    void unauthenticatedAccountDeletionIsBlocked() throws Exception {
        mvc.perform(delete("/api/customer/account")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/accounts/" + designerId))
                .andExpect(status().isForbidden());
        assertTrue(accounts.existsById(designerId));
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }
}
