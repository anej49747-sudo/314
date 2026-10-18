package csit314.renovation.authentication.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.authentication.dto.LoginRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerLoginIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired JdbcTemplate jdbc;

    private void requireTestDatabase() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
    }

    @BeforeEach
    void setup() throws Exception {
        requireTestDatabase();
        accounts.deleteAll();
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"salutation":"Ms","name":"Jane Tan","email":"jane@example.com","password":"SecurePass1!"}
                """)).andExpect(status().isCreated());
    }

    @AfterEach
    void tearDown() {
        requireTestDatabase();
        accounts.deleteAll();
    }

    private record CsrfSession(MockHttpSession session, String header, String token) {}

    private CsrfSession csrf() throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        HttpSession session = result.getRequest().getSession(false);
        return new CsrfSession((MockHttpSession) session, body.get("headerName").asText(), body.get("token").asText());
    }

    private MvcResult login(CsrfSession csrf, String email, String password) throws Exception {
        return mvc.perform(post("/api/login").session(csrf.session())
                .header(csrf.header(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest(email, password)))).andReturn();
    }

    @Test
    void validCustomerLoginSetsServerSessionAndMeUsesAuthenticatedAccount() throws Exception {
        CsrfSession csrf = csrf();
        MvcResult loggedIn = login(csrf, "JANE@EXAMPLE.COM", "SecurePass1!");
        assertEquals(200, loggedIn.getResponse().getStatus());
        JsonNode response = json.readTree(loggedIn.getResponse().getContentAsString());
        assertEquals("CUSTOMER", response.get("role").asText());
        assertEquals("Ms Jane Tan", response.get("displayName").asText());
        assertTrue(response.get("passwordHash") == null);
        assertTrue(response.get("password") == null);
        MockHttpSession session = (MockHttpSession) loggedIn.getRequest().getSession(false);
        assertTrue(session != null);
        mvc.perform(get("/api/auth/me").session(session).param("userId", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.get("id").asLong()))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidCredentialsAndInactiveAccountsAreRejectedWithSamePublicMessage() throws Exception {
        CsrfSession wrongPasswordCsrf = csrf();
        MvcResult wrong = login(wrongPasswordCsrf, "jane@example.com", "Incorrect123!");
        assertEquals(401, wrong.getResponse().getStatus());
        assertEquals("Invalid email or password.", json.readTree(wrong.getResponse().getContentAsString()).get("message").asText());
        mvc.perform(get("/api/auth/me").session(wrongPasswordCsrf.session())).andExpect(status().isUnauthorized());

        jdbc.update("UPDATE user_accounts SET active = false WHERE email = ?", "jane@example.com");
        MvcResult inactive = login(csrf(), "jane@example.com", "SecurePass1!");
        assertEquals(401, inactive.getResponse().getStatus());
        assertEquals("Invalid email or password.", json.readTree(inactive.getResponse().getContentAsString()).get("message").asText());
    }

    @Test
    void roleCannotUseCustomerEndpoint() throws Exception {
        jdbc.update("UPDATE user_accounts SET role = 'INTERIOR_DESIGNER' WHERE email = ?", "jane@example.com");
        MvcResult login = login(csrf(), "jane@example.com", "SecurePass1!");
        assertEquals(200, login.getResponse().getStatus());
        mvc.perform(get("/api/auth/me").session((MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginRequiresValidCsrfTokenAndNoCredentialOverride() throws Exception {
        CsrfSession csrf = csrf();
        mvc.perform(post("/api/login").session(csrf.session()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"jane@example.com","password":"SecurePass1!"}
                        """))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/login").session(csrf.session()).header(csrf.header(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"jane@example.com","password":"SecurePass1!","role":"USER_ADMIN"}
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/me").session(csrf.session())).andExpect(status().isUnauthorized());
    }

    private CsrfSession csrfFor(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return new CsrfSession(session, body.get("headerName").asText(), body.get("token").asText());
    }

    private MockHttpSession loggedInSession() throws Exception {
        MvcResult result = login(csrf(), "jane@example.com", "SecurePass1!");
        assertEquals(200, result.getResponse().getStatus());
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void logoutInvalidatesCurrentSessionAndExpiresBrowserSessionCookie() throws Exception {
        MockHttpSession authenticated = loggedInSession();
        CsrfSession fresh = csrfFor(authenticated);
        mvc.perform(post("/api/logout").session(authenticated)
                        .header(fresh.header(), fresh.token()))
                .andExpect(status().isNoContent())
                .andExpect(result -> assertTrue(result.getResponse().getHeader("Set-Cookie") != null
                        && result.getResponse().getHeader("Set-Cookie").contains("JSESSIONID=")));
        assertTrue(authenticated.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRequiresCsrfAndGetRequestCannotLogOut() throws Exception {
        MockHttpSession authenticated = loggedInSession();
        mvc.perform(post("/api/logout").session(authenticated))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/logout").session(authenticated))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(authenticated))
                .andExpect(status().isOk());
        assertTrue(!authenticated.isInvalid());
    }

    @Test
    void logoutDoesNotInvalidateOtherAuthenticatedSessions() throws Exception {
        MockHttpSession first = loggedInSession();
        MockHttpSession second = loggedInSession();
        CsrfSession fresh = csrfFor(first);
        mvc.perform(post("/api/logout").session(first).header(fresh.header(), fresh.token()))
                .andExpect(status().isNoContent());
        assertTrue(first.isInvalid());
        mvc.perform(get("/api/auth/me").session(second))
                .andExpect(status().isOk());
    }

    @Test
    void customerCanLogInAgainAfterLogout() throws Exception {
        MockHttpSession first = loggedInSession();
        CsrfSession fresh = csrfFor(first);
        mvc.perform(post("/api/logout").session(first).header(fresh.header(), fresh.token()))
                .andExpect(status().isNoContent());
        assertTrue(first.isInvalid());
        MockHttpSession next = loggedInSession();
        mvc.perform(get("/api/auth/me").session(next))
                .andExpect(status().isOk());
    }

    @Test
    void oldCsrfTokenIsRejectedAfterLoginAndSessionRotation() throws Exception {
        CsrfSession csrf = csrf();
        MvcResult login = login(csrf, "jane@example.com", "SecurePass1!");
        assertEquals(200, login.getResponse().getStatus());
        MockHttpSession authenticatedSession = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(post("/api/login").session(authenticatedSession)
                .header(csrf.header(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"jane@example.com","password":"SecurePass1!"}
                        """))
                .andExpect(status().isForbidden());
    }
}
