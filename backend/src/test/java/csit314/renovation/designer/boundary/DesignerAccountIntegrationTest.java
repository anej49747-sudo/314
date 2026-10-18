package csit314.renovation.designer.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
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
class DesignerAccountIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        requireTestDatabase();
        accounts.deleteAll();
        accounts.saveAndFlush(UserAccount.newUserAdmin("Test Admin", "admin@example.com",
                encoder.encode("AdminSecure123!")));
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Test Customer", "customer@example.com",
                encoder.encode("CustomerSecure123!")));
    }

    @AfterEach
    void cleanup() {
        requireTestDatabase();
        accounts.deleteAll();
    }

    private void requireTestDatabase() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
    }

    private record CsrfSession(MockHttpSession session, String headerName, String token) {}

    private CsrfSession csrf(MockHttpSession session) throws Exception {
        var request = get("/api/auth/csrf");
        MvcResult result = mvc.perform(session == null ? request : request.session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode payload = json.readTree(result.getResponse().getContentAsString());
        return new CsrfSession((MockHttpSession) result.getRequest().getSession(false),
                payload.get("headerName").asText(), payload.get("token").asText());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        CsrfSession token = csrf(null);
        MvcResult result = mvc.perform(post("/api/login").session(token.session())
                        .header(token.headerName(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String validRequest() {
        return """
                {"salutation":"Dr","name":"Designer Co","email":"designer@example.com"}
                """;
    }

    @Test
    void adminCreatesDesignerAndPasswordIsShownOnlyInCreationResponse() throws Exception {
        MockHttpSession admin = login("admin@example.com", "AdminSecure123!");
        mvc.perform(get("/api/auth/admin/me").session(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("USER_ADMIN"));
        CsrfSession fresh = csrf(admin);
        MvcResult result = mvc.perform(post("/api/admin/designers").session(admin)
                        .header(fresh.headerName(), fresh.token())
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("INTERIOR_DESIGNER"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.initialPassword").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        assertTrue(result.getResponse().getHeader("Cache-Control").contains("no-store"));
        JsonNode response = json.readTree(result.getResponse().getContentAsString());
        UserAccount stored = accounts.findByEmailIgnoreCase("designer@example.com").orElseThrow();
        assertEquals(UserRole.INTERIOR_DESIGNER, stored.getRole());
        assertTrue(stored.isActive());
        assertEquals("Dr Designer Co", stored.getDisplayName());
        assertFalse(stored.getPasswordHash().equals(response.get("initialPassword").asText()));
        assertTrue(encoder.matches(response.get("initialPassword").asText(), stored.getPasswordHash()));
        // No password retrieval through subsequent account/profile responses.
        mvc.perform(get("/api/auth/admin/me").session(admin))
                .andExpect(jsonPath("$.initialPassword").doesNotExist());
    }

    @Test
    void duplicateAndInvalidRequestsHaveNoAdditionalAccountSideEffects() throws Exception {
        MockHttpSession admin = login("admin@example.com", "AdminSecure123!");
        CsrfSession csrf = csrf(admin);
        mvc.perform(post("/api/admin/designers").session(admin)
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/admin/designers").session(admin)
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Second Designer","email":"DESIGNER@EXAMPLE.COM"}
                                """))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/admin/designers").session(admin)
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/designers").session(admin)
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Designer","email":"new@example.com","role":"USER_ADMIN"}
                                """))
                .andExpect(status().isBadRequest());
        assertEquals(3, accounts.count());
    }

    @Test
    void rejectsUnauthenticatedCustomerAndMissingCsrf() throws Exception {
        CsrfSession anonymous = csrf(null);
        mvc.perform(post("/api/admin/designers").session(anonymous.session())
                        .header(anonymous.headerName(), anonymous.token())
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isUnauthorized());
        MockHttpSession customer = login("customer@example.com", "CustomerSecure123!");
        CsrfSession customerToken = csrf(customer);
        mvc.perform(post("/api/admin/designers").session(customer)
                        .header(customerToken.headerName(), customerToken.token())
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isForbidden());
        MockHttpSession admin = login("admin@example.com", "AdminSecure123!");
        mvc.perform(post("/api/admin/designers").session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isForbidden());
        assertEquals(2, accounts.count());
    }

    @Test
    void revokedAdminRoleOrSuspendedAccountCannotCreateDesignerWithExistingSession() throws Exception {
        MockHttpSession admin = login("admin@example.com", "AdminSecure123!");
        CsrfSession fresh = csrf(admin);
        jdbc.update("UPDATE user_accounts SET active = false WHERE email = 'admin@example.com'");
        mvc.perform(post("/api/admin/designers").session(admin)
                        .header(fresh.headerName(), fresh.token())
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isForbidden());
        assertEquals(2, accounts.count());
    }
}
