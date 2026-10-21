package csit314.renovation.customer.boundary;

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
class CustomerAccountSettingsIntegrationTest {
    private static final String ENDPOINT = "/api/customer/account";
    private static final String PASSWORD = "CustomerSecure123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        safeDatabase();
        accounts.deleteAll();
        accounts.saveAndFlush(UserAccount.newCustomer(Salutation.MS, "Mei Customer", "mei@example.com",
                encoder.encode(PASSWORD)));
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Other Customer", "other@example.com",
                encoder.encode(PASSWORD)));
        UserAccount designer = UserAccount.newInteriorDesigner(null, "Designer", "designer@example.com",
                encoder.encode(PASSWORD));
        designer.replacePasswordHash(encoder.encode(PASSWORD));
        accounts.saveAndFlush(designer);
        accounts.saveAndFlush(UserAccount.newUserAdmin("Admin", "admin@example.com", encoder.encode(PASSWORD)));
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

    private String payload(String name, String email) throws Exception {
        return json.writeValueAsString(Map.of("displayName", name, "email", email));
    }

    @Test
    void customerCanLoadSettingsAndChangeNameWithoutEndingSession() throws Exception {
        UserAccount before = accounts.findByEmailIgnoreCase("mei@example.com").orElseThrow();
        Long accountId = before.getId();
        String passwordHash = before.getPasswordHash();
        MockHttpSession session = login("mei@example.com");
        mvc.perform(get(ENDPOINT).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Mei Customer"))
                .andExpect(jsonPath("$.email").value("mei@example.com"))
                .andExpect(jsonPath("$.signInRequired").value(false))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());
        CsrfSession token = csrf(session);
        mvc.perform(put(ENDPOINT).session(session).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("  Mei Updated  ", "MEI@EXAMPLE.COM")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Mei Updated"))
                .andExpect(jsonPath("$.signInRequired").value(false));
        mvc.perform(get(ENDPOINT).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Mei Updated"));
        UserAccount updated = accounts.findByEmailIgnoreCase("mei@example.com").orElseThrow();
        assertEquals(accountId, updated.getId());
        assertEquals(passwordHash, updated.getPasswordHash());
        assertEquals(Salutation.MS, updated.getSalutation());
        assertEquals("Ms Mei Updated", updated.getDisplayName());
        assertEquals("Other Customer", accounts.findByEmailIgnoreCase("other@example.com").orElseThrow().getName());
    }

    @Test
    void changingEmailEndsOldSessionAndNewAddressCanLogIn() throws Exception {
        MockHttpSession session = login("mei@example.com");
        CsrfSession token = csrf(session);
        mvc.perform(put(ENDPOINT).session(session).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("Mei Customer", " NEW.MEI@Example.com ".strip())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.mei@example.com"))
                .andExpect(jsonPath("$.signInRequired").value(true));
        assertTrue(session.isInvalid());
        assertTrue(accounts.findByEmailIgnoreCase("mei@example.com").isEmpty());
        mvc.perform(get(ENDPOINT).session(login("new.mei@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Mei Customer"));
    }

    @Test
    void rejectsCaseInsensitiveDuplicateAcrossAccountRoles() throws Exception {
        MockHttpSession session = login("mei@example.com");
        CsrfSession token = csrf(session);
        for (String duplicate : new String[] { "OTHER@EXAMPLE.COM", "designer@example.com" }) {
            mvc.perform(put(ENDPOINT).session(session).header(token.header(), token.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload("Different Name", duplicate)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.fieldErrors.email").exists());
        }
        assertEquals("Mei Customer", accounts.findByEmailIgnoreCase("mei@example.com").orElseThrow().getName());
    }

    @Test
    void invalidFieldsAreRejectedWithoutMutatingAccount() throws Exception {
        MockHttpSession session = login("mei@example.com");
        CsrfSession token = csrf(session);
        for (String invalid : new String[] {
                payload("   ", "ok@example.com"),
                payload("x".repeat(161), "ok@example.com"),
                payload("Mei", "not-an-email"),
                payload("Mei", "x".repeat(250) + "@example.com"),
                "{\"email\":\"ok@example.com\"}",
                "{\"displayName\":\"Mei\",\"email\":\"ok@example.com\",\"role\":\"USER_ADMIN\"}"
        }) {
            mvc.perform(put(ENDPOINT).session(session).header(token.header(), token.token())
                            .contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }
        assertEquals("Mei Customer", accounts.findByEmailIgnoreCase("mei@example.com").orElseThrow().getName());
    }

    @Test
    void onlyCustomerWithCsrfCanReadOrUpdateOwnAccount() throws Exception {
        mvc.perform(get(ENDPOINT)).andExpect(status().isUnauthorized());
        mvc.perform(put(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                        .content(payload("No Session", "test@example.com")))
                .andExpect(status().isForbidden());
        MockHttpSession designer = login("designer@example.com");
        mvc.perform(get(ENDPOINT).session(designer)).andExpect(status().isForbidden());
        CsrfSession designerToken = csrf(designer);
        mvc.perform(put(ENDPOINT).session(designer).header(designerToken.header(), designerToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("Bad Change", "designer@example.com")))
                .andExpect(status().isForbidden());
        MockHttpSession admin = login("admin@example.com");
        mvc.perform(get(ENDPOINT).session(admin)).andExpect(status().isForbidden());
        MockHttpSession customer = login("mei@example.com");
        mvc.perform(put(ENDPOINT).session(customer).contentType(MediaType.APPLICATION_JSON)
                        .content(payload("No Csrf", "mei@example.com")))
                .andExpect(status().isForbidden());
    }
}
