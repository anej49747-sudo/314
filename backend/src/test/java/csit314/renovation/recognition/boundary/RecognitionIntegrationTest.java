package csit314.renovation.recognition.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.category.entity.ProjectCategory;
import csit314.renovation.category.repository.ProjectCategoryRepository;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectStatus;
import csit314.renovation.portfolio.entity.ProjectImage;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import csit314.renovation.recognition.repository.DesignerAwardRepository;
import csit314.renovation.recognition.repository.ProjectOfMonthRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
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
class RecognitionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired ProjectCategoryRepository categories;
    @Autowired InteriorDesignProjectRepository projects;
    @Autowired ProjectImageRepository projectImages;
    @Autowired ProjectOfMonthRepository featured;
    @Autowired DesignerAwardRepository awards;
    @Autowired PasswordEncoder encoder;

    private Long projectId;
    private Long secondProjectId;
    private Long designerId;
    private Long otherDesignerId;
    private String currentMonth;

    @BeforeEach
    void prepare() {
        clearRecords();
        accounts.saveAndFlush(UserAccount.newPlatformManager("Manager", "recognition.manager@test.local",
                encoder.encode("ManagerPassword123!")));
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer", "recognition.customer@test.local",
                encoder.encode("CustomerPassword123!")));
        UserAccount designer = accounts.saveAndFlush(UserAccount.newInteriorDesigner(null,
                "Designer", "recognition.designer@test.local", encoder.encode("DesignerPassword123!")));
        UserAccount otherDesigner = accounts.saveAndFlush(UserAccount.newInteriorDesigner(null,
                "Second Designer", "recognition.second@test.local", encoder.encode("DesignerPassword123!")));
        designerId = designer.getId();
        otherDesignerId = otherDesigner.getId();
        ProjectCategory category = categories.saveAndFlush(new ProjectCategory("Test Recognition Category"));
        projectId = projects.saveAndFlush(new InteriorDesignProject(designer, category,
                "Warm Living Room", "A complete portfolio project", ProjectStatus.COMPLETED)).getId();
        secondProjectId = projects.saveAndFlush(new InteriorDesignProject(otherDesigner, category,
                "Bright Kitchen", "A current portfolio project", ProjectStatus.CURRENT)).getId();
        currentMonth = YearMonth.from(LocalDate.now(ZoneId.of("Asia/Singapore"))).toString();
    }

    @AfterEach
    void cleanup() { clearRecords(); }

    private void clearRecords() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        featured.deleteAll();
        awards.deleteAll();
        projects.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();
    }

    private record SessionCsrf(MockHttpSession session, String header, String token) {}

    private SessionCsrf csrf(MockHttpSession session) throws Exception {
        var request = get("/api/auth/csrf");
        MvcResult result = mvc.perform(session == null ? request : request.session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = json.readTree(result.getResponse().getContentAsString());
        return new SessionCsrf((MockHttpSession) result.getRequest().getSession(false),
                data.get("headerName").asText(), data.get("token").asText());
    }

    private SessionCsrf login(String email, String password) throws Exception {
        SessionCsrf token = csrf(null);
        MvcResult result = mvc.perform(post("/api/login").session(token.session())
                .header(token.header(), token.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return csrf((MockHttpSession) result.getRequest().getSession(false));
    }

    @Test
    void managerSelectsReplacesAndRemovesFeatureWithoutChangingProjects() throws Exception {
        SessionCsrf manager = login("recognition.manager@test.local", "ManagerPassword123!");
        mvc.perform(get("/api/manager/recognitions/projects/{month}", currentMonth)
                .session(manager.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.selection").isEmpty());
        mvc.perform(put("/api/manager/recognitions/projects/{month}", currentMonth)
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":" + projectId + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.selection.projectId").value(projectId));
        mvc.perform(get("/api/recognitions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.featuredProject.projectId").value(projectId));
        mvc.perform(put("/api/manager/recognitions/projects/{month}", currentMonth)
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":" + secondProjectId + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.selection.projectId").value(secondProjectId));
        assertEquals(1, featured.count());
        assertEquals(2, projects.count());
        mvc.perform(get("/api/recognitions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.featuredProject.projectId").value(secondProjectId));
        mvc.perform(delete("/api/manager/recognitions/projects/{month}", currentMonth)
                .session(manager.session()).header(manager.header(), manager.token()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/recognitions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.featuredProject").isEmpty());
        assertEquals(2, projects.count());
    }

    @Test
    void publicFeatureIncludesFirstRealPhotoWithoutRequiringProjectDetailsVisit() throws Exception {
        var project = projects.findById(projectId).orElseThrow();
        var second = projectImages.saveAndFlush(new ProjectImage(project,
                "homepage-demo-second.jpg", "image/jpeg", 120, 2));
        var first = projectImages.saveAndFlush(new ProjectImage(project,
                "homepage-demo-first.jpg", "image/jpeg", 120, 1));
        SessionCsrf manager = login("recognition.manager@test.local", "ManagerPassword123!");
        mvc.perform(put("/api/manager/recognitions/projects/{month}", currentMonth)
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":" + projectId + "}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/recognitions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.featuredProject.coverUrl").value(
                        "/api/projects/" + projectId + "/images/" + first.getId()));
        assertEquals(2, projectImages.count());
        assertTrue(second.getId() != null);
    }

    @Test
    void monthlySelectionsDoNotDisplaceDifferentMonthsAndValidateIds() throws Exception {
        SessionCsrf manager = login("recognition.manager@test.local", "ManagerPassword123!");
        String nextMonth = YearMonth.parse(currentMonth).plusMonths(1).toString();
        for (String month : new String[]{currentMonth, nextMonth}) {
            mvc.perform(put("/api/manager/recognitions/projects/{month}", month)
                    .session(manager.session()).header(manager.header(), manager.token())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":" + projectId + "}"))
                    .andExpect(status().isOk());
        }
        assertEquals(2, featured.count());
        mvc.perform(put("/api/manager/recognitions/projects/{month}", "2026-13")
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":" + projectId + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/manager/recognitions/projects/{month}", currentMonth)
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":-5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/manager/recognitions/projects/{month}", currentMonth)
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":99999999}"))
                .andExpect(status().isNotFound());
        assertEquals(2, featured.count());
    }

    @Test
    void designerCanHaveSeveralTitleOnlyAwardsButNotDuplicateTitles() throws Exception {
        SessionCsrf manager = login("recognition.manager@test.local", "ManagerPassword123!");
        var first = mvc.perform(post("/api/manager/recognitions/awards")
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "designerId", designerId, "title", "  Best Space Planning  "))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Best Space Planning"))
                .andReturn();
        Long awardId = json.readTree(first.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/manager/recognitions/awards")
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "designerId", designerId, "title", "Client Choice 2026"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/manager/recognitions/awards")
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "designerId", designerId, "title", "  best space planning  "))))
                .andExpect(status().isConflict());
        assertEquals(2, awards.count());
        mvc.perform(get("/api/recognitions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.awards.length()").value(2));
        mvc.perform(delete("/api/manager/recognitions/awards/{id}", awardId)
                .session(manager.session()).header(manager.header(), manager.token()))
                .andExpect(status().isNoContent());
        assertEquals(1, awards.count());
        mvc.perform(get("/api/manager/recognitions/awards").session(manager.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Client Choice 2026"));
    }

    @Test
    void deniesGuestCustomerWritesAndEnforcesCsrfAndAwardValidation() throws Exception {
        SessionCsrf guest = csrf(null);
        mvc.perform(get("/api/recognitions")).andExpect(status().isOk());
        mvc.perform(get("/api/manager/recognitions/awards")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/manager/recognitions/awards").session(guest.session())
                .header(guest.header(), guest.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"designerId\":1,\"title\":\"No Access\"}"))
                .andExpect(status().isUnauthorized());
        SessionCsrf customer = login("recognition.customer@test.local", "CustomerPassword123!");
        mvc.perform(get("/api/manager/recognitions/awards").session(customer.session()))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/manager/recognitions/projects/{month}", currentMonth)
                .session(customer.session()).header(customer.header(), customer.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":" + projectId + "}"))
                .andExpect(status().isForbidden());
        SessionCsrf manager = login("recognition.manager@test.local", "ManagerPassword123!");
        mvc.perform(post("/api/manager/recognitions/awards").session(manager.session())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"designerId\":" + designerId + ",\"title\":\"Nice Award\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/manager/recognitions/projects/{month}", currentMonth)
                .session(manager.session()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\":" + projectId + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/manager/recognitions/awards").session(manager.session())
                .header(manager.header(), manager.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"designerId\":" + designerId + ",\"title\":\"X\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/manager/recognitions/awards").session(manager.session())
                .header(manager.header(), manager.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"designerId\":999999999,\"title\":\"Valid title\"}"))
                .andExpect(status().isNotFound());
        assertTrue(awards.findAll().isEmpty());
        assertFalse(featured.existsById(currentMonth));
    }

    @Test
    void inactiveDesignersAndTheirProjectsAreHiddenFromPublicRecognition() throws Exception {
        SessionCsrf manager = login("recognition.manager@test.local", "ManagerPassword123!");
        mvc.perform(put("/api/manager/recognitions/projects/{month}", currentMonth)
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":" + projectId + "}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/manager/recognitions/awards")
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "designerId", designerId, "title", "Client Choice"))))
                .andExpect(status().isCreated());
        UserAccount designer = accounts.findById(designerId).orElseThrow();
        designer.setActiveForModeration(false);
        accounts.saveAndFlush(designer);
        mvc.perform(get("/api/recognitions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.featuredProject").isEmpty())
                .andExpect(jsonPath("$.awards").isEmpty());
        // The recognition records are retained for a possible reactivation.
        assertEquals(1, featured.count());
        assertEquals(1, awards.count());
    }

    @Test
    void deletingDesignerOrProjectCascadesRecognitions() throws Exception {
        SessionCsrf manager = login("recognition.manager@test.local", "ManagerPassword123!");
        mvc.perform(put("/api/manager/recognitions/projects/{month}", currentMonth)
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":" + projectId + "}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/manager/recognitions/awards")
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "designerId", otherDesignerId, "title", "Sustainable Materials Award"))))
                .andExpect(status().isCreated());
        // The database's ON DELETE CASCADE removes related rows without deleting other content.
        projects.deleteById(projectId);
        projects.flush();
        assertEquals(0, featured.count());
        accounts.deleteById(otherDesignerId);
        accounts.flush();
        assertEquals(0, awards.count());
        assertEquals(1, accounts.findByRoleAndActiveTrue(csit314.renovation.account.entity.UserRole.INTERIOR_DESIGNER).size());
    }
}
