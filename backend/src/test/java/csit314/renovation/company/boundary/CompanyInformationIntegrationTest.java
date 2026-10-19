package csit314.renovation.company.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.company.repository.DesignerCompanyRepository;
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
class CompanyInformationIntegrationTest {
    private static final String ENDPOINT = "/api/designer/company-information";
    private static final String PASSWORD = "DesignerSecure123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired DesignerCompanyRepository companies;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        safeDatabase();
        companies.deleteAll();
        accounts.deleteAll();
        createDesigner("designer1@example.com", true);
        createDesigner("designer2@example.com", true);
        createDesigner("pending@example.com", false);
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer", "customer@example.com",
                encoder.encode("CustomerSecure123!")));
        accounts.saveAndFlush(UserAccount.newPlatformManager("Manager", "manager@example.com",
                encoder.encode("ManagerSecure123!")));
    }

    @AfterEach
    void cleanup() {
        safeDatabase();
        companies.deleteAll();
        accounts.deleteAll();
    }

    private void safeDatabase() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
    }

    private void createDesigner(String email, boolean passwordChanged) {
        UserAccount designer = UserAccount.newInteriorDesigner(null, "Test Designer", email,
                encoder.encode(PASSWORD));
        if (passwordChanged) designer.replacePasswordHash(encoder.encode(PASSWORD));
        accounts.saveAndFlush(designer);
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
        CsrfSession token = csrf(null);
        MvcResult result = mvc.perform(post("/api/login").session(token.session())
                        .header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String details(String name, String address, String description) throws Exception {
        return json.writeValueAsString(Map.of("companyName", name, "address", address,
                "description", description));
    }

    @Test
    void designerSeesEmptyFormThenCreatesAndUpdatesOnePersistedCompany() throws Exception {
        MockHttpSession designer = login("designer1@example.com", PASSWORD);
        mvc.perform(get(ENDPOINT).session(designer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value(""));
        assertEquals(0, companies.count());

        CsrfSession token = csrf(designer);
        mvc.perform(put(ENDPOINT).session(designer).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("  Bright Spaces  ", "  Singapore  ", "  Modern interiors  ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Bright Spaces"))
                .andExpect(jsonPath("$.address").value("Singapore"));
        assertEquals(1, companies.count());
        mvc.perform(get(ENDPOINT).session(designer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Modern interiors"));

        mvc.perform(put(ENDPOINT).session(designer).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("New Name", "", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("New Name"))
                .andExpect(jsonPath("$.address").value(""));
        assertEquals(1, companies.count());
    }

    @Test
    void oneDesignerCannotReadOrOverwriteAnothersCompany() throws Exception {
        MockHttpSession first = login("designer1@example.com", PASSWORD);
        CsrfSession firstToken = csrf(first);
        mvc.perform(put(ENDPOINT).session(first).header(firstToken.header(), firstToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("Private to First", "", "")))
                .andExpect(status().isOk());

        MockHttpSession second = login("designer2@example.com", PASSWORD);
        mvc.perform(get(ENDPOINT).session(second)).andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value(""));
        CsrfSession secondToken = csrf(second);
        mvc.perform(put(ENDPOINT).session(second).header(secondToken.header(), secondToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("Second Company", "", "")))
                .andExpect(status().isOk());
        mvc.perform(get(ENDPOINT).session(first)).andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Private to First"));
        assertEquals(2, companies.count());
    }

    @Test
    void validatesNameFieldLengthsAndRejectsUnexpectedOwnerIds() throws Exception {
        MockHttpSession designer = login("designer1@example.com", PASSWORD);
        CsrfSession token = csrf(designer);
        mvc.perform(put(ENDPOINT).session(designer).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("  ", "", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.companyName").exists());
        mvc.perform(put(ENDPOINT).session(designer).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("Valid", "x".repeat(301), "")))
                .andExpect(status().isBadRequest());
        mvc.perform(put(ENDPOINT).session(designer).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("Valid", "", "x".repeat(3001))))
                .andExpect(status().isBadRequest());
        mvc.perform(put(ENDPOINT).session(designer).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Valid\",\"designerId\":123}"))
                .andExpect(status().isBadRequest());
        assertEquals(0, companies.count());
    }

    @Test
    void rejectsAnonymousAndCustomerAndMissingCsrf() throws Exception {
        mvc.perform(get(ENDPOINT)).andExpect(status().isUnauthorized());
        CsrfSession anonymous = csrf(null);
        mvc.perform(put(ENDPOINT).session(anonymous.session())
                        .header(anonymous.header(), anonymous.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("Company", "", "")))
                .andExpect(status().isUnauthorized());
        MockHttpSession customer = login("customer@example.com", "CustomerSecure123!");
        mvc.perform(get(ENDPOINT).session(customer)).andExpect(status().isForbidden());
        CsrfSession customerToken = csrf(customer);
        mvc.perform(put(ENDPOINT).session(customer).header(customerToken.header(), customerToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("Company", "", "")))
                .andExpect(status().isForbidden());
        MockHttpSession manager = login("manager@example.com", "ManagerSecure123!");
        mvc.perform(get(ENDPOINT).session(manager)).andExpect(status().isForbidden());
        MockHttpSession designer = login("designer1@example.com", PASSWORD);
        mvc.perform(put(ENDPOINT).session(designer).contentType(MediaType.APPLICATION_JSON)
                        .content(details("Company", "", "")))
                .andExpect(status().isForbidden());
        assertEquals(0, companies.count());
    }

    @Test
    void temporaryPasswordAndDisabledAccountsCannotUseCompanyFeature() throws Exception {
        MockHttpSession pending = login("pending@example.com", PASSWORD);
        mvc.perform(get(ENDPOINT).session(pending)).andExpect(status().isForbidden());
        CsrfSession token = csrf(pending);
        mvc.perform(put(ENDPOINT).session(pending).header(token.header(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("Company", "", "")))
                .andExpect(status().isForbidden());

        MockHttpSession active = login("designer1@example.com", PASSWORD);
        CsrfSession activeToken = csrf(active);
        jdbc.update("UPDATE user_accounts SET active = false WHERE email = 'designer1@example.com'");
        mvc.perform(get(ENDPOINT).session(active)).andExpect(status().isForbidden());
        mvc.perform(put(ENDPOINT).session(active).header(activeToken.header(), activeToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("Company", "", "")))
                .andExpect(status().isForbidden());
        assertEquals(0, companies.count());
    }
}
