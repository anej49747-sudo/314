package csit314.renovation.category.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import csit314.renovation.portfolio.repository.ProjectImageRepository;
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
class CategoryDeletionProjectCorrectionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired ProjectCategoryRepository categories;
    @Autowired InteriorDesignProjectRepository projects;
    @Autowired ProjectImageRepository images;
    @Autowired PasswordEncoder encoder;

    private ProjectCategory category;
    private InteriorDesignProject project;
    private Long designerId;

    @BeforeEach
    void prepare() {
        clearRecords();
        accounts.saveAndFlush(UserAccount.newPlatformManager("Test Manager", "manager@example.com",
                encoder.encode("ManagerSecure123!")));
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Test Customer", "customer@example.com",
                encoder.encode("CustomerSecure123!")));
        UserAccount designer = accounts.saveAndFlush(UserAccount.newInteriorDesigner(null,
                "Test Designer", "designer@example.com", encoder.encode("TempPassword123!")));
        designerId = designer.getId();
        category = categories.saveAndFlush(new ProjectCategory("Residential"));
        project = projects.saveAndFlush(new InteriorDesignProject(designer, category, "Original Home",
                "Original details", ProjectStatus.CURRENT));
        jdbc.update("INSERT INTO project_images (project_id, storage_key, content_type, size_bytes, display_order) "
                + "VALUES (?, ?, 'image/png', 100, 0)", project.getId(), "category-delete-test-image-" + project.getId());
        jdbc.update("INSERT INTO customer_project_favourites (customer_id, project_id) "
                + "SELECT id, ? FROM user_accounts WHERE email = 'customer@example.com'", project.getId());
        jdbc.update("INSERT INTO project_view_counts (project_id, view_count) VALUES (?, 12)", project.getId());
    }

    @AfterEach
    void cleanup() { clearRecords(); }

    private void clearRecords() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        jdbc.update("DELETE FROM customer_project_favourites");
        jdbc.update("DELETE FROM project_view_counts");
        images.deleteAll();
        projects.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();
    }

    private record SessionCsrf(MockHttpSession session, String header, String token) {}

    private SessionCsrf csrf(MockHttpSession session) throws Exception {
        var request = get("/api/auth/csrf");
        MvcResult result = mvc.perform(session == null ? request : request.session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = json.readTree(result.getResponse().getContentAsString());
        return new SessionCsrf((MockHttpSession) result.getRequest().getSession(false),
                data.get("headerName").asText(), data.get("token").asText());
    }

    private SessionCsrf login(String email, String password) throws Exception {
        SessionCsrf token = csrf(null);
        MvcResult result = mvc.perform(post("/api/login").session(token.session())
                .header(token.header(), token.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return csrf((MockHttpSession) result.getRequest().getSession(false));
    }

    @Test
    void deletingUsedCategoryUncategorisesButPreservesProjectImageFavouriteAndViews() throws Exception {
        SessionCsrf manager = login("manager@example.com", "ManagerSecure123!");
        mvc.perform(delete("/api/categories/{id}", category.getId()).session(manager.session())
                .header(manager.header(), manager.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.affectedProjects").value(1));

        assertEquals(0, categories.count());
        assertEquals(1, projects.count());
        InteriorDesignProject retained = projects.findById(project.getId()).orElseThrow();
        assertNull(retained.getCategory());
        assertEquals(designerId, retained.getDesigner().getId());
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM project_images WHERE project_id = ?",
                Long.class, project.getId()).longValue());
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM customer_project_favourites WHERE project_id = ?",
                Long.class, project.getId()).longValue());
        assertEquals(12L, jdbc.queryForObject("SELECT view_count FROM project_view_counts WHERE project_id = ?",
                Long.class, project.getId()).longValue());
        mvc.perform(get("/api/projects")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("Uncategorised"));
        mvc.perform(get("/api/projects/{id}", project.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Uncategorised"));
        mvc.perform(get("/api/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        // Saved projects also render without dereferencing a deleted category.
        SessionCsrf customer = login("customer@example.com", "CustomerSecure123!");
        mvc.perform(get("/api/customer/favourites").session(customer.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("Uncategorised"));
    }

    @Test
    void managerEditsOnlyInformationAndCanReassignUncategorisedProject() throws Exception {
        SessionCsrf manager = login("manager@example.com", "ManagerSecure123!");
        mvc.perform(get("/api/manager/projects").session(manager.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].imageCount").value(1));
        mvc.perform(put("/api/manager/projects/{id}", project.getId()).session(manager.session())
                .header(manager.header(), manager.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("title", "  Corrected Home  ",
                        "description", "  Corrected details  ", "categoryId", category.getId(),
                        "status", "COMPLETED"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Corrected Home"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM project_images WHERE project_id = ?",
                Long.class, project.getId()).longValue());
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM customer_project_favourites WHERE project_id = ?",
                Long.class, project.getId()).longValue());
        assertEquals(12L, jdbc.queryForObject("SELECT view_count FROM project_view_counts WHERE project_id = ?",
                Long.class, project.getId()).longValue());
        assertEquals(designerId, projects.findById(project.getId()).orElseThrow().getDesigner().getId());
        mvc.perform(get("/api/projects")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Corrected Home"));
        // Clearing an assigned category is valid in the manager editor.
        mvc.perform(put("/api/manager/projects/{id}", project.getId()).session(manager.session())
                .header(manager.header(), manager.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Corrected Home\",\"description\":\"Corrected details\","
                        + "\"categoryId\":null,\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoryName").value("Uncategorised"));
        assertNull(projects.findById(project.getId()).orElseThrow().getCategory());
    }

    @Test
    void restrictsManagerMutationsAndValidatesRequestBodies() throws Exception {
        SessionCsrf guest = csrf(null);
        mvc.perform(delete("/api/categories/{id}", category.getId()).session(guest.session())
                .header(guest.header(), guest.token())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/manager/projects")).andExpect(status().isUnauthorized());
        SessionCsrf customer = login("customer@example.com", "CustomerSecure123!");
        mvc.perform(delete("/api/categories/{id}", category.getId()).session(customer.session())
                .header(customer.header(), customer.token())).andExpect(status().isForbidden());
        mvc.perform(get("/api/manager/projects").session(customer.session())).andExpect(status().isForbidden());
        mvc.perform(put("/api/manager/projects/{id}", project.getId()).session(customer.session())
                .header(customer.header(), customer.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Unauthorized\"}"))
                .andExpect(status().isForbidden());
        SessionCsrf manager = login("manager@example.com", "ManagerSecure123!");
        mvc.perform(delete("/api/categories/{id}", category.getId()).session(manager.session()))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/manager/projects/{id}", project.getId()).session(manager.session())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Unprotected\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/manager/projects/{id}", project.getId()).session(manager.session())
                .header(manager.header(), manager.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\" \" ,\"description\":\"valid\",\"status\":\"CURRENT\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/manager/projects/{id}", project.getId()).session(manager.session())
                .header(manager.header(), manager.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Valid\",\"description\":\"valid\",\"status\":\"CURRENT\",\"designerId\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/manager/projects/{id}", project.getId()).session(manager.session())
                .header(manager.header(), manager.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Valid\",\"description\":\"valid\",\"status\":\"CURRENT\",\"categoryId\":999999999}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/manager/projects/{id}", 99999999).session(manager.session())
                .header(manager.header(), manager.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Valid\",\"description\":\"valid\",\"status\":\"CURRENT\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/categories/{id}", 99999999).session(manager.session())
                .header(manager.header(), manager.token())).andExpect(status().isNotFound());
        assertTrue(categories.existsById(category.getId()));
        assertEquals("Original Home", projects.findById(project.getId()).orElseThrow().getTitle());
    }
}
