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
import csit314.renovation.category.entity.ProjectCategory;
import csit314.renovation.category.repository.ProjectCategoryRepository;
import csit314.renovation.company.repository.DesignerCompanyRepository;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectStatus;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
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
class DesignerProfileIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired DesignerCompanyRepository companies;
    @Autowired ProjectCategoryRepository categories;
    @Autowired InteriorDesignProjectRepository projects;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        safeDatabase();
        projects.deleteAll();
        companies.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();
    }

    @AfterEach
    void cleanup() {
        safeDatabase();
        projects.deleteAll();
        companies.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();
    }

    private void safeDatabase() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
    }

    private UserAccount designer(String name, String email) {
        return accounts.saveAndFlush(UserAccount.newInteriorDesigner(null, name, email,
                encoder.encode("TempPassword123!")));
    }

    private void company(UserAccount designer, String name, String address, String description) {
        // Avoid a detached @MapsId one-to-one reference in test fixture creation.
        jdbc.update("INSERT INTO designer_companies (designer_id, company_name, address, description) "
                + "VALUES (?, ?, ?, ?)", designer.getId(), name, address, description);
    }

    private String url(UserAccount designer) {
        return "/api/designers/" + designer.getId();
    }

    @Test
    void publicProfileIncludesCompanyInformationAndOnlyOwnProjectSummaries() throws Exception {
        UserAccount studio = designer("Individual Name", "studio@example.com");
        UserAccount other = designer("Other Designer", "other@example.com");
        company(studio, "Northline Interiors", "Orchard, Singapore", "About our studio");
        ProjectCategory category = categories.saveAndFlush(new ProjectCategory("Residential"));
        projects.saveAndFlush(new InteriorDesignProject(studio, category, "Current kitchen",
                "Private detailed project description", ProjectStatus.CURRENT));
        projects.saveAndFlush(new InteriorDesignProject(studio, category, "Completed living room",
                "Another private project description", ProjectStatus.COMPLETED));
        projects.saveAndFlush(new InteriorDesignProject(other, category, "Other portfolio project",
                "Another designer's description", ProjectStatus.CURRENT));

        mvc.perform(get(url(studio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studio.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Northline Interiors"))
                .andExpect(jsonPath("$.address").value("Orchard, Singapore"))
                .andExpect(jsonPath("$.description").value("About our studio"))
                .andExpect(jsonPath("$.projects.length()").value(2))
                .andExpect(jsonPath("$.projects[0].title").value("Completed living room"))
                .andExpect(jsonPath("$.projects[0].categoryName").value("Residential"))
                .andExpect(jsonPath("$.projects[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.projects[0].id").isNumber())
                .andExpect(jsonPath("$.projects[1].status").value("CURRENT"))
                .andExpect(jsonPath("$.projects[0].description").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.active").doesNotExist());
    }

    @Test
    void designerWithoutCompanyOrProjectsStillHasPublicProfile() throws Exception {
        UserAccount individual = designer("Taylor Wong", "taylor@example.com");
        mvc.perform(get(url(individual)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Taylor Wong"))
                .andExpect(jsonPath("$.address").isEmpty())
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.projects.length()").value(0));
    }

    @Test
    void hidesSuspendedNonDesignerAndNonexistentAccountsAsNotFound() throws Exception {
        UserAccount inactive = designer("Suspended", "suspended@example.com");
        company(inactive, "Suspended Company", "Singapore", "Not public");
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", inactive.getId());
        UserAccount customer = accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer",
                "customer@example.com", encoder.encode("CustomerSecure123!")));
        UserAccount manager = accounts.saveAndFlush(UserAccount.newPlatformManager("Manager",
                "manager@example.com", encoder.encode("ManagerSecure123!")));

        mvc.perform(get(url(inactive))).andExpect(status().isNotFound());
        mvc.perform(get(url(customer))).andExpect(status().isNotFound());
        mvc.perform(get(url(manager))).andExpect(status().isNotFound());
        mvc.perform(get("/api/designers/999999999")).andExpect(status().isNotFound());
    }

    @Test
    void designerWithTemporaryPasswordCanViewPublicProfile() throws Exception {
        UserAccount designer = designer("Pending Designer", "pending@example.com");
        MvcResult csrfResult = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(csrfResult.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        MvcResult login = mvc.perform(post("/api/login").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "pending@example.com",
                                "password", "TempPassword123!"))))
                .andExpect(status().isOk()).andReturn();
        mvc.perform(get(url(designer)).session((MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pending Designer"));
    }
}
