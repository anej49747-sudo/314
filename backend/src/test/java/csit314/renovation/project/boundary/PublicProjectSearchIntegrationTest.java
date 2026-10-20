package csit314.renovation.project.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.category.entity.ProjectCategory;
import csit314.renovation.category.repository.ProjectCategoryRepository;
import csit314.renovation.company.repository.DesignerCompanyRepository;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectStatus;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
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
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicProjectSearchIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired ProjectCategoryRepository categories;
    @Autowired InteriorDesignProjectRepository projects;
    @Autowired ProjectImageRepository images;
    @Autowired DesignerCompanyRepository companies;
    @Autowired PasswordEncoder encoder;

    private ProjectCategory category;

    @BeforeEach
    void prepare() {
        clearDisposableRecords();
        category = categories.saveAndFlush(new ProjectCategory("Residential"));
    }

    @AfterEach
    void cleanup() {
        clearDisposableRecords();
    }

    private void clearDisposableRecords() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        images.deleteAll();
        projects.deleteAll();
        companies.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();
    }

    private UserAccount designer(String name, String email) {
        return accounts.saveAndFlush(UserAccount.newInteriorDesigner(null, name, email,
                encoder.encode("TempPassword123!")));
    }

    private InteriorDesignProject project(UserAccount designer, String title, String description) {
        return projects.saveAndFlush(new InteriorDesignProject(designer, category, title,
                description, ProjectStatus.COMPLETED));
    }

    private JsonNode search(String query) throws Exception {
        String endpoint = "/api/projects";
        String response = query == null
                ? mvc.perform(get(endpoint)).andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()
                : mvc.perform(get(endpoint).param("q", query)).andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    @Test
    void anonymousInitialListingShowsOnlyActiveDesignerProjectsWithSafeFields() throws Exception {
        UserAccount active = designer("Alex Lee", "alex@example.com");
        jdbc.update("INSERT INTO designer_companies (designer_id, company_name, address, description) "
                        + "VALUES (?, ?, ?, ?)", active.getId(), "Northline Interiors",
                "Singapore", "Company-only description");
        project(active, "Modern Kitchen", "New cabinetry");
        UserAccount individual = designer("Jamie Tan", "jamie@example.com");
        project(individual, "Coastal Living Room", "Bright finishes");
        UserAccount suspended = designer("Suspended", "suspended@example.com");
        project(suspended, "Hidden Studio", "Should never be public");
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", suspended.getId());

        JsonNode results = search(null);
        assertEquals(2, results.size());
        assertEquals("Coastal Living Room", results.get(0).get("title").asText());
        assertEquals("Jamie Tan", results.get(0).get("designer").get("name").asText());
        assertEquals("Modern Kitchen", results.get(1).get("title").asText());
        assertEquals("Northline Interiors", results.get(1).get("designer").get("name").asText());
        assertEquals("Residential", results.get(1).get("categoryName").asText());
        assertEquals("COMPLETED", results.get(1).get("status").asText());
        assertEquals("New cabinetry", results.get(1).get("description").asText());
        assertFalse(results.get(1).has("createdAt"));
        assertFalse(results.get(1).has("passwordHash"));
        assertFalse(results.get(1).get("designer").has("email"));
        assertFalse(results.get(1).get("designer").has("passwordHash"));
        assertFalse(results.get(1).get("designer").has("active"));
    }

    @Test
    void titleSearchIsCaseInsensitivePartialAndDoesNotSearchDescriptions() throws Exception {
        UserAccount designer = designer("Sam", "sam@example.com");
        project(designer, "Minimalist Kitchen Renewal", "Calm, simple finishes");
        project(designer, "Urban Loft", "Kitchen planning is included here");

        JsonNode matches = search("  kItChEn  ");
        assertEquals(1, matches.size());
        assertEquals("Minimalist Kitchen Renewal", matches.get(0).get("title").asText());
        assertTrue(search("no matches").isEmpty());
        assertEquals(2, search("  ").size());
    }

    @Test
    void percentAndUnderscoreAreLiteralTitleCharactersNotSqlWildcards() throws Exception {
        UserAccount designer = designer("Sam", "sam@example.com");
        project(designer, "100% Completed Home", "One hundred percent");
        project(designer, "100 Other Projects", "No percent in the title");
        project(designer, "Office_A", "Underscore");
        assertEquals(1, search("%").size());
        assertEquals("100% Completed Home", search("%").get(0).get("title").asText());
        assertEquals(1, search("_").size());
    }

    @Test
    void designerWithPendingInitialPasswordCanBrowsePublicProjects() throws Exception {
        UserAccount designer = designer("Pending", "pending@example.com");
        project(designer, "Public Interior", "Public project description");
        MvcResult csrfResult = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(csrfResult.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        MvcResult login = mvc.perform(post("/api/login").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "pending@example.com",
                                "password", "TempPassword123!"))))
                .andExpect(status().isOk()).andReturn();

        String result = mvc.perform(get("/api/projects")
                        .session((MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals("Public Interior", json.readTree(result).get(0).get("title").asText());
    }

    @Test
    void publicListingDoesNotExposeAWriteEndpoint() throws Exception {
        mvc.perform(post("/api/projects")).andExpect(status().isForbidden());
    }
}
