package csit314.renovation.account.boundary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AdminAccountSearchIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void prepare() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        accounts.deleteAll();
        String hash = encoder.encode("StrongPassword123!");
        accounts.saveAndFlush(UserAccount.newUserAdmin("Admin", "admin@example.com", hash));
        accounts.saveAndFlush(UserAccount.newPlatformManager("Manager", "manager@example.com", hash));
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Alice Jones", "alice@example.com", hash));
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Bob Smith", "bob@example.com", hash));
        UserAccount inactive = accounts.saveAndFlush(UserAccount.newInteriorDesigner(null,
                "Alice Design", "designer@example.com", hash));
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", inactive.getId());
    }

    @AfterEach
    void cleanup() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        accounts.deleteAll();
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult csrfResponse = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(csrfResponse.getResponse().getContentAsString());
        MockHttpSession initial = (MockHttpSession) csrfResponse.getRequest().getSession(false);
        MvcResult result = mvc.perform(post("/api/login").session(initial)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email,
                                "password", "StrongPassword123!"))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void adminSeesOnlyEligibleAccountsIncludingInactiveButNoSecrets() throws Exception {
        var response = mvc.perform(get("/api/admin/accounts").session(login("admin@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.accounts.length()").value(3))
                .andExpect(jsonPath("$.accounts[?(@.role == 'USER_ADMIN')]").isEmpty())
                .andExpect(jsonPath("$.accounts[?(@.role == 'PLATFORM_MANAGER')]").isEmpty())
                .andExpect(jsonPath("$.accounts[?(@.active == false)]")
                        .value(org.hamcrest.Matchers.hasSize(1)))
                .andReturn();
        String jsonText = response.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(jsonText.contains("passwordHash"));
        org.junit.jupiter.api.Assertions.assertFalse(jsonText.contains("passwordChangeRequired"));
        org.junit.jupiter.api.Assertions.assertTrue(response.getResponse().getHeader("Cache-Control")
                .contains("no-store"));
    }

    @Test
    void searchByCaseInsensitiveNameOrEmailAndCombineFilters() throws Exception {
        MockHttpSession admin = login("admin@example.com");
        mvc.perform(get("/api/admin/accounts").session(admin).param("q", "  ALICE  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/admin/accounts").session(admin).param("q", "DESIGNER@")
                        .param("role", "INTERIOR_DESIGNER").param("status", "inactive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.accounts[0].active").value(false));
        mvc.perform(get("/api/admin/accounts").session(admin).param("q", "not-here"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/admin/accounts").session(admin).param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsUnauthorisedUsersAndInvalidParameters() throws Exception {
        mvc.perform(get("/api/admin/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/accounts").session(login("alice@example.com")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/accounts").session(login("manager@example.com")))
                .andExpect(status().isForbidden());
        MockHttpSession admin = login("admin@example.com");
        mvc.perform(get("/api/admin/accounts").session(admin).param("role", "USER_ADMIN"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/accounts").session(admin).param("status", "suspended"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/accounts").session(admin).param("page", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/accounts").session(admin).param("q", "x".repeat(101)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paginateTwentyAccountsPerPageWithoutDuplicatingRecords() throws Exception {
        String hash = encoder.encode("OtherStrongPassword123!");
        for (int index = 0; index < 21; index++) {
            accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer " + index,
                    "extra" + index + "@example.com", hash));
        }
        MockHttpSession admin = login("admin@example.com");
        mvc.perform(get("/api/admin/accounts").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(24))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.accounts.length()").value(20));
        mvc.perform(get("/api/admin/accounts").session(admin).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts.length()").value(4));
    }
}
