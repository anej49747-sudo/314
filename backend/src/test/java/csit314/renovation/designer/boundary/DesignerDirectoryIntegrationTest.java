package csit314.renovation.designer.boundary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.company.repository.DesignerCompanyRepository;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
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
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DesignerDirectoryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired DesignerCompanyRepository companies;
    @Autowired InteriorDesignProjectRepository projects;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        safeDatabase();
        projects.deleteAll();
        companies.deleteAll();
        accounts.deleteAll();
    }

    @AfterEach
    void cleanup() {
        safeDatabase();
        projects.deleteAll();
        companies.deleteAll();
        accounts.deleteAll();
    }

    private void safeDatabase() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
    }

    private UserAccount designer(String name, String email) {
        return accounts.saveAndFlush(UserAccount.newInteriorDesigner(null, name, email,
                encoder.encode("TempPassword123!")));
    }

    private void addCompany(UserAccount designer, String name, String address, String description) {
        // This listing test needs persisted company data, not a JPA company-creation workflow.
        jdbc.update("INSERT INTO designer_companies (designer_id, company_name, address, description) "
                + "VALUES (?, ?, ?, ?)", designer.getId(), name, address, description);
    }

    @Test
    void listsActiveApprovedDesignersAndCompanyOrIndividualNames() throws Exception {
        UserAccount studio = designer("Individual Name", "studio@example.com");
        addCompany(studio, "Northline Interiors", "Orchard, Singapore", "Residential interiors");

        designer("Alex Tan", "alex@example.com"); // Profile not completed; still listed.
        UserAccount inactive = designer("Inactive Person", "inactive@example.com");
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", inactive.getId());
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer", "customer@example.com",
                encoder.encode("CustomerSecure123!")));
        accounts.saveAndFlush(UserAccount.newPlatformManager("Manager", "manager@example.com",
                encoder.encode("ManagerSecure123!")));

        mvc.perform(get("/api/designers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alex Tan"))
                .andExpect(jsonPath("$[0].address").isEmpty())
                .andExpect(jsonPath("$[1].id").value(studio.getId().intValue()))
                .andExpect(jsonPath("$[1].name").value("Northline Interiors"))
                .andExpect(jsonPath("$[1].address").value("Orchard, Singapore"))
                .andExpect(jsonPath("$[1].description").value("Residential interiors"))
                .andExpect(jsonPath("$[1].email").doesNotExist())
                .andExpect(jsonPath("$[1].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[1].role").doesNotExist())
                .andExpect(jsonPath("$[1].active").doesNotExist());
    }

    @Test
    void anonymousVisitorCanBrowseEmptyDirectory() throws Exception {
        mvc.perform(get("/api/designers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void temporaryPasswordDesignerCanBrowseThePublicDirectory() throws Exception {
        designer("Pending Designer", "pending@example.com");
        MvcResult csrfResult = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(csrfResult.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        MvcResult loginResult = mvc.perform(post("/api/login").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "pending@example.com",
                                "password", "TempPassword123!"))))
                .andExpect(status().isOk()).andReturn();
        mvc.perform(get("/api/designers")
                        .session((MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void doesNotShowSuspendedDesignersEvenWhenTheyHaveCompanyInformation() throws Exception {
        UserAccount inactive = designer("Unavailable Designer", "unavailable@example.com");
        addCompany(inactive, "Inactive Studio", "Singapore", "Unavailable");
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", inactive.getId());

        mvc.perform(get("/api/designers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
