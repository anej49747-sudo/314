package csit314.renovation.category.boundary;

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
import csit314.renovation.category.repository.ProjectCategoryRepository;
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
class CategoryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired ProjectCategoryRepository categories;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        safeDatabase();
        categories.deleteAll();
        accounts.deleteAll();
        accounts.saveAndFlush(UserAccount.newPlatformManager("Test Manager", "manager@example.com",
                encoder.encode("ManagerSecure123!")));
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Test Customer", "customer@example.com",
                encoder.encode("CustomerSecure123!")));
        accounts.saveAndFlush(UserAccount.newUserAdmin("Test Admin", "admin@example.com",
                encoder.encode("AdminSecure123!")));
    }

    @AfterEach
    void cleanup() {
        safeDatabase();
        categories.deleteAll();
        accounts.deleteAll();
    }

    private void safeDatabase() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
    }

    private record CsrfSession(MockHttpSession session, String header, String token) {}

    private CsrfSession csrf(MockHttpSession session) throws Exception {
        var request = get("/api/auth/csrf");
        MvcResult result = mvc.perform(session == null ? request : request.session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = json.readTree(result.getResponse().getContentAsString());
        return new CsrfSession((MockHttpSession) result.getRequest().getSession(false),
                data.get("headerName").asText(), data.get("token").asText());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        CsrfSession csrf = csrf(null);
        MvcResult result = mvc.perform(post("/api/login")
                        .session(csrf.session()).header(csrf.header(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void managerCanCreateAndListPersistedCategory() throws Exception {
        MockHttpSession manager = login("manager@example.com", "ManagerSecure123!");
        mvc.perform(get("/api/auth/manager/me").session(manager))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("PLATFORM_MANAGER"));
        CsrfSession token = csrf(manager);
        mvc.perform(post("/api/categories").session(manager)
                        .header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  Modern  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Modern"))
                .andExpect(jsonPath("$.id").isNumber());
        assertEquals(1, categories.count());
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Modern"));
    }

    @Test
    void blankDuplicateAndUnexpectedFieldsAreRejected() throws Exception {
        MockHttpSession manager = login("manager@example.com", "ManagerSecure123!");
        CsrfSession token = csrf(manager);
        mvc.perform(post("/api/categories").session(manager).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/categories").session(manager).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Modern\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/categories").session(manager).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" MODERN \"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/categories").session(manager).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Another\",\"role\":\"PLATFORM_MANAGER\"}"))
                .andExpect(status().isBadRequest());
        assertEquals(1, categories.count());
    }

    @Test
    void rejectsAnonymousCustomerAdminAndMissingCsrf() throws Exception {
        CsrfSession anonymous = csrf(null);
        mvc.perform(post("/api/categories").session(anonymous.session())
                        .header(anonymous.header(), anonymous.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Modern\"}"))
                .andExpect(status().isUnauthorized());
        MockHttpSession customer = login("customer@example.com", "CustomerSecure123!");
        CsrfSession customerToken = csrf(customer);
        mvc.perform(post("/api/categories").session(customer).header(customerToken.header(), customerToken.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Modern\"}"))
                .andExpect(status().isForbidden());
        MockHttpSession admin = login("admin@example.com", "AdminSecure123!");
        CsrfSession adminToken = csrf(admin);
        mvc.perform(post("/api/categories").session(admin).header(adminToken.header(), adminToken.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Modern\"}"))
                .andExpect(status().isForbidden());
        MockHttpSession manager = login("manager@example.com", "ManagerSecure123!");
        mvc.perform(post("/api/categories").session(manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Modern\"}"))
                .andExpect(status().isForbidden());
        assertEquals(0, categories.count());
    }

    @Test
    void suspendedManagerCannotCreateWithExistingSession() throws Exception {
        MockHttpSession manager = login("manager@example.com", "ManagerSecure123!");
        CsrfSession token = csrf(manager);
        jdbc.update("UPDATE user_accounts SET active = false WHERE email = 'manager@example.com'");
        mvc.perform(post("/api/categories").session(manager).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Modern\"}"))
                .andExpect(status().isUnauthorized());
        assertEquals(0, categories.count());
    }
}
