package csit314.renovation.portfolio.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.category.entity.ProjectCategory;
import csit314.renovation.category.repository.ProjectCategoryRepository;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectStatus;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
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
class PortfolioIntegrationTest {
    private static final String ENDPOINT = "/api/designer/projects";
    private static final String PASSWORD = "DesignerSecure123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired ProjectCategoryRepository categories;
    @Autowired InteriorDesignProjectRepository projects;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    private UserAccount first;
    private UserAccount second;
    private ProjectCategory category;

    @BeforeEach
    void prepare() {
        safeDatabase();
        projects.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();

        first = createDesigner("first@example.com", true);
        second = createDesigner("second@example.com", true);
        createDesigner("pending@example.com", false);
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer", "customer@example.com",
                encoder.encode("CustomerSecure123!")));
        accounts.saveAndFlush(UserAccount.newPlatformManager("Manager", "manager@example.com",
                encoder.encode("ManagerSecure123!")));
        category = categories.saveAndFlush(new ProjectCategory("Residential"));
    }

    @AfterEach
    void cleanup() {
        safeDatabase();
        projects.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();
    }

    private void safeDatabase() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
    }

    private UserAccount createDesigner(String email, boolean passwordChanged) {
        UserAccount designer = UserAccount.newInteriorDesigner(null, "Test Designer", email,
                encoder.encode(PASSWORD));
        if (passwordChanged) designer.replacePasswordHash(encoder.encode(PASSWORD));
        return accounts.saveAndFlush(designer);
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult csrfResult = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode csrf = json.readTree(csrfResult.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        MvcResult loginResult = mvc.perform(post("/api/login").session(session)
                        .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    @Test
    void showsEmptyPortfolioWhenDesignerHasNoProjects() throws Exception {
        MockHttpSession session = login("first@example.com", PASSWORD);
        mvc.perform(get(ENDPOINT).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listsOnlyOwnProjectsWithCategoryAndStatus() throws Exception {
        projects.saveAndFlush(new InteriorDesignProject(first, category, "Current kitchen",
                "Kitchen remodel", ProjectStatus.CURRENT));
        projects.saveAndFlush(new InteriorDesignProject(first, category, "Completed living room",
                "Warm minimal design", ProjectStatus.COMPLETED));
        projects.saveAndFlush(new InteriorDesignProject(second, category, "Other designer project",
                "This must remain private", ProjectStatus.CURRENT));

        MockHttpSession firstSession = login("first@example.com", PASSWORD);
        mvc.perform(get(ENDPOINT).session(firstSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Completed living room"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].categoryId").value(category.getId().intValue()))
                .andExpect(jsonPath("$[0].categoryName").value("Residential"))
                .andExpect(jsonPath("$[0].description").value("Warm minimal design"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[1].title").value("Current kitchen"));

        MockHttpSession secondSession = login("second@example.com", PASSWORD);
        mvc.perform(get(ENDPOINT).session(secondSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Other designer project"));

        // Query parameters must never override the authenticated owner's identity.
        mvc.perform(get(ENDPOINT).session(secondSession).param("designerId", first.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        assertEquals(3, projects.count());
    }

    @Test
    void rejectsAnonymousNonDesignerAndPendingPasswordAccounts() throws Exception {
        mvc.perform(get(ENDPOINT)).andExpect(status().isUnauthorized());
        mvc.perform(get(ENDPOINT).session(login("customer@example.com", "CustomerSecure123!")))
                .andExpect(status().isForbidden());
        mvc.perform(get(ENDPOINT).session(login("manager@example.com", "ManagerSecure123!")))
                .andExpect(status().isForbidden());
        mvc.perform(get(ENDPOINT).session(login("pending@example.com", PASSWORD)))
                .andExpect(status().isForbidden());
        // The read-only story does not expose a project-creation operation.
        mvc.perform(post(ENDPOINT).session(login("first@example.com", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspendedDesignerCannotListPortfolioWithExistingSession() throws Exception {
        MockHttpSession session = login("first@example.com", PASSWORD);
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", first.getId());
        mvc.perform(get(ENDPOINT).session(session)).andExpect(status().isForbidden());
    }
}
