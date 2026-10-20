package csit314.renovation.favourite.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class CustomerSavedProjectsIntegrationTest {
    private static final String PASSWORD = "TestPassword123!";
    private static final String LIST_URL = "/api/customer/favourites";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired ProjectCategoryRepository categories;
    @Autowired InteriorDesignProjectRepository projects;
    @Autowired PasswordEncoder encoder;

    private UserAccount designer;
    private ProjectCategory category;
    private InteriorDesignProject firstProject;

    @BeforeEach
    void prepare() {
        clearDisposableRecords();
        designer = accounts.saveAndFlush(UserAccount.newInteriorDesigner(null,
                "Designer", "designer@example.com", encoder.encode(PASSWORD)));
        category = categories.saveAndFlush(new ProjectCategory("Residential"));
        firstProject = projects.saveAndFlush(new InteriorDesignProject(designer, category,
                "Apartment One", "Project description", ProjectStatus.COMPLETED));
    }

    @AfterEach
    void cleanup() {
        clearDisposableRecords();
    }

    private void clearDisposableRecords() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        jdbc.update("DELETE FROM customer_project_favourites");
        projects.deleteAll();
        jdbc.update("DELETE FROM designer_companies");
        categories.deleteAll();
        accounts.deleteAll();
    }

    private void customer(String email) {
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer", email,
                encoder.encode(PASSWORD)));
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult initial = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(initial.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) initial.getRequest().getSession(false);
        MvcResult login = mvc.perform(post("/api/login").session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }

    private JsonNode csrf(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private void save(MockHttpSession session, Long projectId) throws Exception {
        JsonNode token = csrf(session);
        mvc.perform(put(LIST_URL + "/" + projectId).session(session)
                .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isNoContent());
    }

    @Test
    void onlyCustomerCanReadOwnSavedProjects() throws Exception {
        customer("first@example.com");
        customer("second@example.com");
        MockHttpSession first = login("first@example.com");
        MockHttpSession second = login("second@example.com");
        save(first, firstProject.getId());

        mvc.perform(get(LIST_URL)).andExpect(status().isUnauthorized());
        mvc.perform(get(LIST_URL).session(login("designer@example.com")))
                .andExpect(status().isForbidden());
        mvc.perform(get(LIST_URL).session(second))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(LIST_URL).session(first))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(firstProject.getId()))
                .andExpect(jsonPath("$[0].title").value("Apartment One"))
                .andExpect(jsonPath("$[0].categoryName").value("Residential"))
                .andExpect(jsonPath("$[0].designer.name").value("Designer"))
                .andExpect(jsonPath("$[0].designer.email").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void showsNewestSavedFirstWithCompanyDisplayNameAndRemovalUpdatesList() throws Exception {
        customer("first@example.com");
        MockHttpSession session = login("first@example.com");
        InteriorDesignProject second = projects.saveAndFlush(new InteriorDesignProject(
                designer, category, "Apartment Two", "Second description", ProjectStatus.CURRENT));
        jdbc.update("INSERT INTO designer_companies (designer_id, company_name, description) "
                + "VALUES (?, ?, ?)", designer.getId(), "Studio North", "Studio details");
        save(session, firstProject.getId());
        save(session, second.getId());

        mvc.perform(get(LIST_URL).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second.getId()))
                .andExpect(jsonPath("$[0].status").value("CURRENT"))
                .andExpect(jsonPath("$[0].designer.name").value("Studio North"))
                .andExpect(jsonPath("$[1].id").value(firstProject.getId()));
        JsonNode token = csrf(session);
        mvc.perform(delete(LIST_URL + "/" + second.getId()).session(session)
                .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isNoContent());
        mvc.perform(get(LIST_URL).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(firstProject.getId()));
    }

    @Test
    void savedProjectsOfInactiveDesignersAreHiddenButCanStillBeRemoved() throws Exception {
        customer("first@example.com");
        MockHttpSession session = login("first@example.com");
        save(session, firstProject.getId());
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", designer.getId());

        mvc.perform(get(LIST_URL).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM customer_project_favourites",
                Integer.class));
        JsonNode token = csrf(session);
        mvc.perform(delete(LIST_URL + "/" + firstProject.getId()).session(session)
                .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM customer_project_favourites",
                Integer.class));
    }
}
