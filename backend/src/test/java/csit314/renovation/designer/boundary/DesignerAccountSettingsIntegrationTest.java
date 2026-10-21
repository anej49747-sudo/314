package csit314.renovation.designer.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.Salutation;
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
class DesignerAccountSettingsIntegrationTest {
    private static final String ENDPOINT = "/api/designer/account";
    private static final String PASSWORD = "DesignerSecure123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        safeDatabase();
        accounts.deleteAll();
        UserAccount first = UserAccount.newInteriorDesigner(Salutation.MS, "Alex Designer", "alex@example.com",
                encoder.encode(PASSWORD));
        first.replacePasswordHash(encoder.encode(PASSWORD));
        accounts.saveAndFlush(first);
        UserAccount second = UserAccount.newInteriorDesigner(null, "Other Designer", "other@example.com",
                encoder.encode(PASSWORD));
        second.replacePasswordHash(encoder.encode(PASSWORD));
        accounts.saveAndFlush(second);
        accounts.saveAndFlush(UserAccount.newInteriorDesigner(null, "Pending Designer", "pending@example.com",
                encoder.encode(PASSWORD)));
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer", "customer@example.com",
                encoder.encode(PASSWORD)));
    }

    @AfterEach
    void cleanup() {
        safeDatabase();
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

    private MockHttpSession login(String email) throws Exception {
        CsrfSession token = csrf(null);
        MvcResult result = mvc.perform(post("/api/login").session(token.session())
                        .header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String payload(String displayName, String email) throws Exception {
        return json.writeValueAsString(Map.of("displayName", displayName, "email", email));
    }

    @Test
    void designerLoadsRawNameAndSavesNameWithoutChangingSessionOrOtherFields() throws Exception {
        MockHttpSession session = login("alex@example.com");
        mvc.perform(get(ENDPOINT).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alex Designer"))
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.signInRequired").value(false));
        CsrfSession token = csrf(session);
        mvc.perform(put(ENDPOINT).session(session).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("  Updated Designer  ", "ALEX@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Designer"))
                .andExpect(jsonPath("$.signInRequired").value(false));
        mvc.perform(get(ENDPOINT).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Designer"));
        UserAccount updated = accounts.findByEmailIgnoreCase("alex@example.com").orElseThrow();
        assertEquals(Salutation.MS, updated.getSalutation());
        assertEquals("Ms Updated Designer", updated.getDisplayName());
        assertEquals("Other Designer", accounts.findByEmailIgnoreCase("other@example.com").orElseThrow().getName());
    }

    @Test
    void changingSignInEmailInvalidatesOldSessionAndPermitsLoginWithNewEmail() throws Exception {
        MockHttpSession session = login("alex@example.com");
        CsrfSession token = csrf(session);
        mvc.perform(put(ENDPOINT).session(session).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("Alex Designer", "  NEW.EMAIL@Example.COM  ".strip())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.email@example.com"))
                .andExpect(jsonPath("$.signInRequired").value(true));
        assertTrue(session.isInvalid());
        assertTrue(accounts.findByEmailIgnoreCase("alex@example.com").isEmpty());
        mvc.perform(get(ENDPOINT).session(login("new.email@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alex Designer"));
    }

    @Test
    void rejectsDuplicateEmailCaseInsensitiveAndLeavesAccountUnchanged() throws Exception {
        MockHttpSession session = login("alex@example.com");
        CsrfSession token = csrf(session);
        mvc.perform(put(ENDPOINT).session(session).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("Updated", "OTHER@EXAMPLE.COM")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        assertEquals("Alex Designer", accounts.findByEmailIgnoreCase("alex@example.com").orElseThrow().getName());
    }

    @Test
    void rejectsBlankMalformedOversizedAndUnexpectedAccountFields() throws Exception {
        MockHttpSession session = login("alex@example.com");
        CsrfSession token = csrf(session);
        for (String invalid : new String[] {
                payload("  ", "ok@example.com"),
                payload("X".repeat(161), "ok@example.com"),
                payload("Valid", "not-an-email"),
                payload("Valid", "x".repeat(250) + "@example.com"),
                "{\"email\":\"ok@example.com\"}",
                "{\"displayName\":\"Valid\",\"email\":\"ok@example.com\",\"role\":\"USER_ADMIN\"}"
        }) {
            mvc.perform(put(ENDPOINT).session(session).header(token.header(), token.token())
                            .contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }
        assertEquals("Alex Designer", accounts.findByEmailIgnoreCase("alex@example.com").orElseThrow().getName());
    }

    @Test
    void protectsAgainstVisitorCustomerPendingDesignerAndMissingCsrf() throws Exception {
        mvc.perform(get(ENDPOINT)).andExpect(status().isUnauthorized());
        mvc.perform(put(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                        .content(payload("Someone", "test@example.com")))
                .andExpect(status().isForbidden());
        MockHttpSession customer = login("customer@example.com");
        mvc.perform(get(ENDPOINT).session(customer)).andExpect(status().isForbidden());
        CsrfSession customerToken = csrf(customer);
        mvc.perform(put(ENDPOINT).session(customer).header(customerToken.header(), customerToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("Customer changed", "customer@example.com")))
                .andExpect(status().isForbidden());
        MockHttpSession pending = login("pending@example.com");
        mvc.perform(get(ENDPOINT).session(pending)).andExpect(status().isForbidden());
        MockHttpSession designer = login("alex@example.com");
        mvc.perform(put(ENDPOINT).session(designer).contentType(MediaType.APPLICATION_JSON)
                        .content(payload("No CSRF", "alex@example.com")))
                .andExpect(status().isForbidden());
    }
}
