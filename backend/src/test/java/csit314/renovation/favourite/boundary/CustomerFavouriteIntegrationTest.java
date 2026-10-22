package csit314.renovation.favourite.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class CustomerFavouriteIntegrationTest {
    private static final String PASSWORD = "TestPassword123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired ProjectCategoryRepository categories;
    @Autowired InteriorDesignProjectRepository projects;
    @Autowired PasswordEncoder encoder;

    private InteriorDesignProject project;

    @BeforeEach
    void prepare() {
        clearDisposableRecords();
        UserAccount designer = accounts.saveAndFlush(UserAccount.newInteriorDesigner(
                null, "Designer", "designer@example.com", encoder.encode(PASSWORD)));
        ProjectCategory category = categories.saveAndFlush(new ProjectCategory("Residential"));
        project = projects.saveAndFlush(new InteriorDesignProject(designer, category,
                "Contemporary Home", "Renovation details", ProjectStatus.COMPLETED));
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

    private UserAccount customer(String email) {
        return accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer", email,
                encoder.encode(PASSWORD)));
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult initialCsrf = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(initialCsrf.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) initialCsrf.getRequest().getSession(false);
        MvcResult login = mvc.perform(post("/api/login").session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }

    private JsonNode csrf(MockHttpSession session) throws Exception {
        MvcResult response = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(response.getResponse().getContentAsString());
    }

    private String url(Long projectId) {
        return "/api/customer/favourites/" + projectId;
    }

    private void save(MockHttpSession session, Long projectId) throws Exception {
        JsonNode token = csrf(session);
        mvc.perform(put(url(projectId)).session(session)
                .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isNoContent());
    }

    private void remove(MockHttpSession session, Long projectId) throws Exception {
        JsonNode token = csrf(session);
        mvc.perform(delete(url(projectId)).session(session)
                .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isNoContent());
    }

    @Test
    void customerCanSaveCheckAndRemoveRepeatedlyWithoutDuplicateRows() throws Exception {
        customer("customer@example.com");
        MockHttpSession session = login("customer@example.com");
        mvc.perform(get(url(project.getId())).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(false));

        save(session, project.getId());
        save(session, project.getId());
        mvc.perform(get(url(project.getId())).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM customer_project_favourites",
                Integer.class));

        remove(session, project.getId());
        remove(session, project.getId());
        mvc.perform(get(url(project.getId())).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(false));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM customer_project_favourites",
                Integer.class));
    }

    @Test
    void savedStateAndRemovalBelongOnlyToTheAuthenticatedCustomer() throws Exception {
        customer("first@example.com");
        customer("second@example.com");
        MockHttpSession first = login("first@example.com");
        MockHttpSession second = login("second@example.com");
        save(first, project.getId());
        mvc.perform(get(url(project.getId())).session(second))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(false));
        remove(second, project.getId());
        mvc.perform(get(url(project.getId())).session(first))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
        save(second, project.getId());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM customer_project_favourites",
                Integer.class));
        remove(first, project.getId());
        mvc.perform(get(url(project.getId())).session(second))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
    }

    @Test
    void unauthenticatedAndNonCustomerAccountsCannotReadOrWriteFavourites() throws Exception {
        mvc.perform(get(url(project.getId()))).andExpect(status().isUnauthorized());
        MvcResult anonymousCsrf = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(anonymousCsrf.getResponse().getContentAsString());
        MockHttpSession anon = (MockHttpSession) anonymousCsrf.getRequest().getSession(false);
        mvc.perform(put(url(project.getId())).session(anon)
                .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isUnauthorized());

        MockHttpSession designer = login("designer@example.com");
        mvc.perform(get(url(project.getId())).session(designer))
                .andExpect(status().isForbidden());
        token = csrf(designer);
        mvc.perform(put(url(project.getId())).session(designer)
                .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isForbidden());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM customer_project_favourites",
                Integer.class));
    }

    @Test
    void writesRequireCsrfEvenWithCustomerSession() throws Exception {
        customer("customer@example.com");
        MockHttpSession session = login("customer@example.com");
        mvc.perform(put(url(project.getId())).session(session)).andExpect(status().isForbidden());
        save(session, project.getId());
        mvc.perform(delete(url(project.getId())).session(session)).andExpect(status().isForbidden());
        mvc.perform(get(url(project.getId())).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
    }

    @Test
    void missingOrNonPublicProjectCannotBeSavedButFormerFavouriteCanBeRemoved() throws Exception {
        customer("customer@example.com");
        MockHttpSession session = login("customer@example.com");
        mvc.perform(get(url(9999999L)).session(session)).andExpect(status().isNotFound());
        JsonNode token = csrf(session);
        mvc.perform(put(url(9999999L)).session(session)
                .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isNotFound());
        save(session, project.getId());
        jdbc.update("UPDATE user_accounts SET active = false WHERE email = ?", "designer@example.com");
        mvc.perform(get(url(project.getId())).session(session)).andExpect(status().isNotFound());
        token = csrf(session);
        mvc.perform(put(url(project.getId())).session(session)
                .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isNotFound());
        remove(session, project.getId());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM customer_project_favourites",
                Integer.class));
    }

    @Test
    void inactiveCustomerCannotModifyFavouritesUsingAnExistingSession() throws Exception {
        UserAccount account = customer("customer@example.com");
        MockHttpSession session = login("customer@example.com");
        JsonNode token = csrf(session);
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", account.getId());
        mvc.perform(put(url(project.getId())).session(session)
                .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isUnauthorized());
        assertTrue(session.isInvalid());
        mvc.perform(get(url(project.getId()))).andExpect(status().isUnauthorized());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM customer_project_favourites",
                Integer.class));
    }
}
