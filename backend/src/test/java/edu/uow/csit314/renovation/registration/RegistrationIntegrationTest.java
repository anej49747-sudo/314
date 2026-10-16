package edu.uow.csit314.renovation.registration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.uow.csit314.renovation.account.UserAccount;
import edu.uow.csit314.renovation.account.UserAccountRepository;
import edu.uow.csit314.renovation.account.UserRole;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    private void requireTestDatabase() {
        String database = jdbc.queryForObject("SELECT current_database()", String.class);
        assertTrue(database != null && database.endsWith("_test"), "Refusing to touch non-test database: " + database);
    }

    @BeforeEach
    void clearTestAccounts() {
        requireTestDatabase();
        accounts.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        requireTestDatabase();
        accounts.deleteAll();
    }

    private static String payload(String email) {
        return """
                {"salutation":"Ms","name":"Jane Tan","email":"%s","password":"SecurePass1!"}
                """.formatted(email);
    }

    @Test
    void registersActiveCustomerInPostgresWithArgon2AndSafeResponse() throws Exception {
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("JANE@EXAMPLE.COM")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName", equalTo("Ms Jane Tan")))
                .andExpect(jsonPath("$.email", equalTo("jane@example.com")))
                .andExpect(jsonPath("$.role", equalTo("CUSTOMER")))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        List<UserAccount> saved = accounts.findAll();
        assertEquals(1, saved.size());
        UserAccount account = saved.getFirst();
        assertTrue(account.isActive());
        assertEquals(UserRole.CUSTOMER, account.getRole());
        assertNotEquals("SecurePass1!", account.getPasswordHash());
        assertTrue(account.getPasswordHash().startsWith("$argon2id$"));
        assertTrue(encoder.matches("SecurePass1!", account.getPasswordHash()));
        assertTrue(account.getId() > 0);
        assertTrue(account.getCreatedAt() != null);
    }

    @Test
    void caseInsensitiveDuplicatesReturnConflictAndNoSecondAccount() throws Exception {
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("Jane@Example.Com"))).andExpect(status().isCreated());
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("jane@example.com"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        assertEquals(1, accounts.count());
    }

    @Test
    void rejectsMissingEmailInvalidPasswordAndSalutation() throws Exception {
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"name":"Jane","password":"SecurePass1!"}
                  """)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"name":"Jane","email":"jane@example.com","password":"short"}
                  """)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"salutation":"King","name":"Jane","email":"jane@example.com","password":"SecurePass1!"}
                  """)).andExpect(status().isBadRequest());
        assertEquals(0, accounts.count());
    }

    @Test
    void disallowsClientRoleOverrideAndPrivateEndpoints() throws Exception {
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"name":"Jane","email":"jane@example.com","password":"SecurePass1!","role":"USER_ADMIN"}
                  """)).andExpect(status().isBadRequest());
        assertEquals(0, accounts.count());
        mvc.perform(get("/api/users/15")).andExpect(status().isForbidden());
    }

    @Test
    void competingRequestsYieldExactlyOneAccount() throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Integer> register = () -> {
                start.await();
                return mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("simultaneous@example.com"))).andReturn().getResponse().getStatus();
            };
            Future<Integer> first = executor.submit(register);
            Future<Integer> second = executor.submit(register);
            start.countDown();
            List<Integer> statuses = List.of(first.get(), second.get());
            assertTrue(statuses.contains(201));
            assertTrue(statuses.contains(409));
            assertEquals(1, accounts.count());
        }
    }
}
