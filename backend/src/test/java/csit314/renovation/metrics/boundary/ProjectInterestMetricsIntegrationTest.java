package csit314.renovation.metrics.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import csit314.renovation.company.repository.DesignerCompanyRepository;
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
class ProjectInterestMetricsIntegrationTest {
    private static final String DESIGNER_PASSWORD = "DemoDesigner123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired DesignerCompanyRepository companies;
    @Autowired ProjectCategoryRepository categories;
    @Autowired InteriorDesignProjectRepository projects;
    @Autowired PasswordEncoder encoder;

    private UserAccount firstDesigner;
    private UserAccount secondDesigner;
    private InteriorDesignProject firstProject;
    private InteriorDesignProject otherProject;
    private UserAccount customerOne;
    private UserAccount customerTwo;

    @BeforeEach
    void prepare() {
        clearDisposableData();
        firstDesigner = designer("Designer One", "metric-designer1@example.com");
        secondDesigner = designer("Designer Two", "metric-designer2@example.com");
        customerOne = accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer One",
                "metric-customer1@example.com", encoder.encode("CustomerSecure123!")));
        customerTwo = accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer Two",
                "metric-customer2@example.com", encoder.encode("CustomerSecure123!")));
        ProjectCategory category = categories.saveAndFlush(new ProjectCategory("Residential"));
        firstProject = projects.saveAndFlush(new InteriorDesignProject(firstDesigner, category,
                "First public project", "Public details", ProjectStatus.COMPLETED));
        otherProject = projects.saveAndFlush(new InteriorDesignProject(secondDesigner, category,
                "Other designer project", "Second public details", ProjectStatus.CURRENT));
    }

    @AfterEach
    void cleanup() { clearDisposableData(); }

    private void clearDisposableData() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        // Both project_view_counts and customer_project_favourites cascade on project deletion.
        projects.deleteAll();
        companies.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();
    }

    private UserAccount designer(String name, String email) {
        UserAccount account = UserAccount.newInteriorDesigner(null, name, email,
                encoder.encode(DESIGNER_PASSWORD));
        account.replacePasswordHash(encoder.encode(DESIGNER_PASSWORD));
        return accounts.saveAndFlush(account);
    }

    private MockHttpSession designerSession(String email) throws Exception {
        MvcResult tokenResponse = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(tokenResponse.getResponse().getContentAsString());
        MvcResult result = mvc.perform(post("/api/login")
                        .session((MockHttpSession) tokenResponse.getRequest().getSession(false))
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email,
                                "password", DESIGNER_PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private long views(Long projectId) {
        return jdbc.queryForObject("SELECT COALESCE((SELECT view_count FROM project_view_counts "
                + "WHERE project_id = ?), 0)", Long.class, projectId);
    }

    @Test
    void anonymousViewCountsOncePerBrowserSessionAndPersistsAcrossSessions() throws Exception {
        MvcResult first = mvc.perform(get("/api/projects/" + firstProject.getId()))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession firstSession = (MockHttpSession) first.getRequest().getSession(false);
        assertNotNull(firstSession, "Anonymous project visitors must receive a browser session");
        assertEquals(1, views(firstProject.getId()));

        mvc.perform(get("/api/projects/" + firstProject.getId()).session(firstSession))
                .andExpect(status().isOk());
        mvc.perform(get("/api/projects").session(firstSession)).andExpect(status().isOk());
        assertEquals(1, views(firstProject.getId()), "Reloads and list browsing must not add views");

        MvcResult second = mvc.perform(get("/api/projects/" + firstProject.getId()))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession secondSession = (MockHttpSession) second.getRequest().getSession(false);
        mvc.perform(get("/api/projects/" + otherProject.getId()).session(firstSession))
                .andExpect(status().isOk());
        assertEquals(2, views(firstProject.getId()));
        assertEquals(1, views(otherProject.getId()));
        mvc.perform(get("/api/projects/" + firstProject.getId()).session(secondSession))
                .andExpect(status().isOk());
        assertEquals(2, views(firstProject.getId()));
    }

    @Test
    void missingOrInactivePublicProjectsDoNotCreateViewRecords() throws Exception {
        mvc.perform(get("/api/projects/999999999")).andExpect(status().isNotFound());
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", secondDesigner.getId());
        mvc.perform(get("/api/projects/" + otherProject.getId()))
                .andExpect(status().isNotFound());
        assertEquals(0, views(firstProject.getId()));
        assertEquals(0, views(otherProject.getId()));
    }

    @Test
    void portfolioShowsOnlyOwnersMetricsAndCurrentFavourites() throws Exception {
        // A view by an anonymous visitor is durable, and favourite counts use current rows.
        mvc.perform(get("/api/projects/" + firstProject.getId())).andExpect(status().isOk());
        jdbc.update("INSERT INTO customer_project_favourites (customer_id, project_id) VALUES (?, ?)",
                customerOne.getId(), firstProject.getId());
        jdbc.update("INSERT INTO customer_project_favourites (customer_id, project_id) VALUES (?, ?)",
                customerTwo.getId(), firstProject.getId());
        jdbc.update("INSERT INTO customer_project_favourites (customer_id, project_id) VALUES (?, ?)",
                customerOne.getId(), otherProject.getId());

        MockHttpSession first = designerSession("metric-designer1@example.com");
        mvc.perform(get("/api/designer/projects").session(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(firstProject.getId().intValue()))
                .andExpect(jsonPath("$[0].viewCount").value(1))
                .andExpect(jsonPath("$[0].favouriteCount").value(2));

        // Remove one save: current favourites decrease, historical views do not.
        jdbc.update("DELETE FROM customer_project_favourites WHERE customer_id = ? AND project_id = ?",
                customerTwo.getId(), firstProject.getId());
        mvc.perform(get("/api/designer/projects").session(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].viewCount").value(1))
                .andExpect(jsonPath("$[0].favouriteCount").value(1));

        MockHttpSession second = designerSession("metric-designer2@example.com");
        mvc.perform(get("/api/designer/projects").session(second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(otherProject.getId().intValue()))
                .andExpect(jsonPath("$[0].viewCount").value(0))
                .andExpect(jsonPath("$[0].favouriteCount").value(1));
        mvc.perform(get("/api/designer/projects")).andExpect(status().isUnauthorized());
    }

    @Test
    void deletingAProjectCascadesItsAggregateViewRecord() throws Exception {
        mvc.perform(get("/api/projects/" + firstProject.getId())).andExpect(status().isOk());
        assertEquals(1, views(firstProject.getId()));
        projects.deleteById(firstProject.getId());
        projects.flush();
        assertEquals(0, views(firstProject.getId()));
    }
}
