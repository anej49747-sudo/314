package csit314.renovation.account.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class AccountManagementIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    private Long customerId;
    private Long adminId;

    @BeforeEach
    void prepare() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        accounts.deleteAll();
        String hash = passwords.encode("StrongPassword123!");
        adminId = accounts.saveAndFlush(UserAccount.newUserAdmin("Admin", "admin@example.com", hash)).getId();
        accounts.saveAndFlush(UserAccount.newPlatformManager("Manager", "manager@example.com", hash));
        customerId = accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer", "customer@example.com", hash)).getId();
        accounts.saveAndFlush(UserAccount.newInteriorDesigner(null, "Designer", "designer@example.com", hash));
    }

    @AfterEach
    void cleanup() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        accounts.deleteAll();
    }

    private record Login(MockHttpSession session, String header, String token) {}

    private Login csrf() throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return new Login((MockHttpSession) result.getRequest().getSession(false),
                body.get("headerName").asText(), body.get("token").asText());
    }

    private MvcResult attempt(Login csrf, String email, String password) throws Exception {
        return mvc.perform(post("/api/login").session(csrf.session())
                .header(csrf.header(), csrf.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password)))).andReturn();
    }

    private Login login(String email) throws Exception {
        Login initial = csrf();
        MvcResult result = attempt(initial, email, "StrongPassword123!");
        assertEquals(200, result.getResponse().getStatus());
        return new Login((MockHttpSession) result.getRequest().getSession(false), null, null);
    }

    private Login tokenFor(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return new Login((MockHttpSession) result.getRequest().getSession(false),
                body.get("headerName").asText(), body.get("token").asText());
    }

    @Test
    void suspensionBlocksExistingSessionAndLoginWhileReactivationRestoresNewLogin() throws Exception {
        Login customer = login("customer@example.com");
        Login admin = tokenFor(login("admin@example.com").session());
        String uri = "/api/admin/accounts/" + customerId;
        mvc.perform(put(uri + "/suspend").session(admin.session())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Repeated misconduct\"}"))
                .andExpect(status().isForbidden()); // CSRF required
        assertTrue(accounts.findById(customerId).orElseThrow().isActive());
        mvc.perform(put(uri + "/suspend").session(admin.session()).header(admin.header(), admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Repeated misconduct\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        assertFalse(accounts.findById(customerId).orElseThrow().isActive());
        mvc.perform(get("/api/auth/session").session(customer.session()))
                .andExpect(status().isUnauthorized());
        assertTrue(customer.session().isInvalid());
        assertEquals(401, attempt(csrf(), "customer@example.com", "StrongPassword123!")
                .getResponse().getStatus());
        mvc.perform(put(uri + "/reactivate").session(admin.session())
                .header(admin.header(), admin.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
        assertEquals(200, attempt(csrf(), "customer@example.com", "StrongPassword123!")
                .getResponse().getStatus());
        Long audits = jdbc.queryForObject("SELECT count(*) FROM account_moderation_events WHERE account_id = ?",
                Long.class, customerId);
        assertEquals(2L, audits);
    }

    @Test
    void scopeValidationAndNoPrivilegedAccountActions() throws Exception {
        Login admin = tokenFor(login("admin@example.com").session());
        String target = "/api/admin/accounts/" + customerId;
        mvc.perform(put(target + "/suspend").session(admin.session())
                .header(admin.header(), admin.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/accounts/" + adminId + "/suspend").session(admin.session())
                .header(admin.header(), admin.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"No\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(put(target + "/reactivate").session(admin.session())
                .header(admin.header(), admin.token())).andExpect(status().isConflict());
        Login customer = tokenFor(login("customer@example.com").session());
        mvc.perform(put(target + "/suspend").session(customer.session())
                .header(customer.header(), customer.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"No\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get(target + "/login-history")).andExpect(status().isUnauthorized());
    }

    @Test
    void capturesRealSuccessfulAndFailedLoginsOnlyForKnownAccounts() throws Exception {
        attempt(csrf(), "CUSTOMER@example.com", "badPassword");
        attempt(csrf(), "notregistered@example.com", "badPassword");
        Login admin = login("admin@example.com");
        String uri = "/api/admin/accounts/" + customerId + "/login-history";
        mvc.perform(get(uri).session(admin.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.events[0].successful").value(false))
                .andExpect(jsonPath("$.events[0].occurredAt").exists());
        mvc.perform(get(uri).session(admin.session()).param("outcome", "success"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        login("customer@example.com");
        mvc.perform(get(uri).session(admin.session()).param("outcome", "success"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(uri).session(admin.session()).param("outcome", "invalid"))
                .andExpect(status().isBadRequest());
        String contents = mvc.perform(get(uri).session(admin.session())).andReturn().getResponse().getContentAsString();
        assertFalse(contents.contains("password"));
        assertFalse(contents.contains("session"));
        Long count = jdbc.queryForObject("SELECT count(*) FROM user_login_events WHERE account_id = ?",
                Long.class, customerId);
        assertEquals(2L, count);
    }
}
