package csit314.renovation.authentication.boundary;

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
import csit314.renovation.account.repository.UserAccountRepository;
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
class DesignerLoginIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    private void requireTestDatabase() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
    }

    @BeforeEach
    void setup() {
        requireTestDatabase();
        accounts.deleteAll();
        accounts.saveAndFlush(UserAccount.newInteriorDesigner(null, "Designer", "designer@example.com",
                encoder.encode("TempPassword1!")));
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer", "customer@example.com",
                encoder.encode("CustomerPass1!")));
    }

    @AfterEach
    void tearDown() {
        requireTestDatabase();
        accounts.deleteAll();
    }

    record Csrf(MockHttpSession session, String header, String token) {}

    private Csrf csrf(MockHttpSession session) throws Exception {
        MvcResult result = (session == null ? mvc.perform(get("/api/auth/csrf"))
                : mvc.perform(get("/api/auth/csrf").session(session))).andExpect(status().isOk()).andReturn();
        JsonNode response = json.readTree(result.getResponse().getContentAsString());
        return new Csrf((MockHttpSession) result.getRequest().getSession(false),
                response.get("headerName").asText(), response.get("token").asText());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        Csrf csrf = csrf(null);
        MvcResult result = mvc.perform(post("/api/login").session(csrf.session())
                .header(csrf.header(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new csit314.renovation.authentication.dto.LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void initialDesignerCanLogInButCannotOpenPartnerAreaBeforeChangingPassword() throws Exception {
        MockHttpSession session = login("DESIGNER@EXAMPLE.COM", "TempPassword1!");
        mvc.perform(get("/api/auth/designer/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeRequired").value(true));
        mvc.perform(get("/api/designer/area").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("INTERIOR_DESIGNER"))
                .andExpect(jsonPath("$.passwordChangeRequired").value(true));
    }

    @Test
    void firstLoginPasswordSetupRequiresCsrfThenUnlocksPartnerArea() throws Exception {
        MockHttpSession session = login("designer@example.com", "TempPassword1!");
        Csrf csrf = csrf(session);
        String payload = """
                {"newPassword":"NewPassword1!"}
                """;
        mvc.perform(post("/api/auth/change-password").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/change-password").session(session)
                .header(csrf.header(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"newPassword":""}
                """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/change-password").session(session)
                .header(csrf.header(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isNoContent());
        UserAccount account = accounts.findByEmailIgnoreCase("designer@example.com").orElseThrow();
        assertFalse(account.isPasswordChangeRequired());
        assertTrue(encoder.matches("NewPassword1!", account.getPasswordHash()));
        mvc.perform(get("/api/designer/area").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/designer/me").session(session))
                .andExpect(jsonPath("$.passwordChangeRequired").value(false));
        // Password setup is allowed only once, even if the authenticated session remains valid.
        mvc.perform(post("/api/auth/change-password").session(session)
                .header(csrf.header(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"newPassword":"DifferentPass2!"}
                """))
                .andExpect(status().isForbidden());
        // Updated Argon2id hash is used for new logins, not the one-time credential.
        Csrf oldCsrf = csrf(null);
        mvc.perform(post("/api/login").session(oldCsrf.session())
                .header(oldCsrf.header(), oldCsrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"designer@example.com","password":"TempPassword1!"}
                        """))
                .andExpect(status().isUnauthorized());
        MockHttpSession newSession = login("designer@example.com", "NewPassword1!");
        mvc.perform(get("/api/designer/area").session(newSession)).andExpect(status().isOk());
    }

    @Test
    void rejectsWeakPasswordAndReuseWithoutChangingStoredCredentials() throws Exception {
        MockHttpSession session = login("designer@example.com", "TempPassword1!");
        Csrf csrf = csrf(session);
        mvc.perform(post("/api/auth/change-password").session(session)
                .header(csrf.header(), csrf.token()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"newPassword":"short"}
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/change-password").session(session)
                .header(csrf.header(), csrf.token()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"newPassword":"TempPassword1!"}
                        """))
                .andExpect(status().isBadRequest());
        assertTrue(accounts.findByEmailIgnoreCase("designer@example.com").orElseThrow().isPasswordChangeRequired());
    }

    @Test
    void designerCanLogOutWhileInitialPasswordChangeIsPending() throws Exception {
        MockHttpSession session = login("designer@example.com", "TempPassword1!");
        Csrf token = csrf(session);
        mvc.perform(post("/api/logout").session(session)
                .header(token.header(), token.token()))
                .andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/designer/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/designer/area")).andExpect(status().isUnauthorized());

        // Logout must not clear the requirement or change the existing credentials.
        assertTrue(accounts.findByEmailIgnoreCase("designer@example.com")
                .orElseThrow().isPasswordChangeRequired());
        MockHttpSession nextSession = login("designer@example.com", "TempPassword1!");
        mvc.perform(get("/api/auth/designer/me").session(nextSession))
                .andExpect(jsonPath("$.passwordChangeRequired").value(true));
    }

    @Test
    void designerLogoutRejectsMissingCsrfAndGetRequests() throws Exception {
        MockHttpSession session = login("designer@example.com", "TempPassword1!");
        mvc.perform(post("/api/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/designer/me").session(session)).andExpect(status().isOk());
        assertFalse(session.isInvalid());
    }

    @Test
    void designerCanLogOutAfterSettingPermanentPassword() throws Exception {
        MockHttpSession session = login("designer@example.com", "TempPassword1!");
        Csrf beforePasswordChange = csrf(session);
        mvc.perform(post("/api/auth/change-password").session(session)
                .header(beforePasswordChange.header(), beforePasswordChange.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"newPassword":"NewPassword1!"}
                        """))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/designer/area").session(session)).andExpect(status().isOk());

        Csrf beforeLogout = csrf(session);
        mvc.perform(post("/api/logout").session(session)
                .header(beforeLogout.header(), beforeLogout.token()))
                .andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/designer/area")).andExpect(status().isUnauthorized());
        MockHttpSession nextSession = login("designer@example.com", "NewPassword1!");
        mvc.perform(get("/api/designer/area").session(nextSession)).andExpect(status().isOk());
    }

    @Test
    void customerCannotChangeDesignerPasswordOrAccessDesignerArea() throws Exception {
        MockHttpSession session = login("customer@example.com", "CustomerPass1!");
        Csrf csrf = csrf(session);
        mvc.perform(post("/api/auth/change-password").session(session)
                .header(csrf.header(), csrf.token()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"newPassword":"AnotherPass1!"}
                        """))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/designer/area").session(session)).andExpect(status().isForbidden());
    }
}
