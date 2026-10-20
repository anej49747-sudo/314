package csit314.renovation.project.boundary;

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
class PublicProjectDetailsIntegrationTest {
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

    private InteriorDesignProject project(UserAccount designer, ProjectStatus status) {
        ProjectCategory category = categories.saveAndFlush(new ProjectCategory("Residential"));
        return projects.saveAndFlush(new InteriorDesignProject(designer, category,
                "Warm Modern Living Room", "A complete project description.\n\nMaterials and scope.", status));
    }

    private String url(InteriorDesignProject project) {
        return "/api/projects/" + project.getId();
    }

    @Test
    void anonymousVisitorSeesFullProjectDetailsAndPublicCompanyName() throws Exception {
        UserAccount studio = designer("Avery Tan", "avery@example.com");
        // Do not persist a detached @MapsId company fixture through JPA.
        jdbc.update("INSERT INTO designer_companies (designer_id, company_name, address, description) "
                        + "VALUES (?, ?, ?, ?)", studio.getId(), "Northline Interiors",
                "Orchard, Singapore", "Private company description");
        InteriorDesignProject project = project(studio, ProjectStatus.COMPLETED);

        mvc.perform(get(url(project)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId().intValue()))
                .andExpect(jsonPath("$.title").value("Warm Modern Living Room"))
                .andExpect(jsonPath("$.description").value("A complete project description.\n\nMaterials and scope."))
                .andExpect(jsonPath("$.categoryName").value("Residential"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.designer.id").value(studio.getId().intValue()))
                .andExpect(jsonPath("$.designer.name").value("Northline Interiors"))
                .andExpect(jsonPath("$.designer.address").value("Orchard, Singapore"))
                .andExpect(jsonPath("$.designer.email").doesNotExist())
                .andExpect(jsonPath("$.designer.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.designer.active").doesNotExist())
                .andExpect(jsonPath("$.designer.description").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());
    }

    @Test
    void individualDesignerWithoutCompanyIsStillVisibleAndCurrent() throws Exception {
        UserAccount individual = designer("Taylor Wong", "taylor@example.com");
        InteriorDesignProject project = project(individual, ProjectStatus.CURRENT);

        mvc.perform(get(url(project)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CURRENT"))
                .andExpect(jsonPath("$.designer.name").value("Taylor Wong"))
                .andExpect(jsonPath("$.designer.address").isEmpty());
    }

    @Test
    void hidesProjectsOfSuspendedDesigners() throws Exception {
        UserAccount suspended = designer("Suspended", "suspended@example.com");
        InteriorDesignProject project = project(suspended, ProjectStatus.COMPLETED);
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", suspended.getId());

        mvc.perform(get(url(project))).andExpect(status().isNotFound());
    }

    @Test
    void missingProjectReturnsNotFound() throws Exception {
        mvc.perform(get("/api/projects/999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void designerWithTemporaryPasswordCanStillReadPublicProjects() throws Exception {
        UserAccount designer = designer("Pending Designer", "pending@example.com");
        InteriorDesignProject project = project(designer, ProjectStatus.CURRENT);
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

        mvc.perform(get(url(project)).session((MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Warm Modern Living Room"));
    }
}
