package csit314.renovation.moodboard.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import csit314.renovation.portfolio.entity.ProjectImage;
import csit314.renovation.portfolio.entity.ProjectStatus;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
import java.util.LinkedHashMap;
import java.util.List;
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

/** PostgreSQL lifecycle, private access, image references and optimistic concurrency. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerMoodboardIntegrationTest {
    private static final String ROOT = "/api/customer/moodboards";
    private static final String PASSWORD = "MoodboardTest123!";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired ProjectCategoryRepository categories;
    @Autowired InteriorDesignProjectRepository projects;
    @Autowired ProjectImageRepository photos;
    @Autowired PasswordEncoder encoder;

    private Long photoId;
    private Long projectId;
    private Long ownerId;

    @BeforeEach
    void setup() {
        cleanup();
        UserAccount owner = accounts.saveAndFlush(UserAccount.newCustomer(null, "Moodboard Owner",
                "board.owner@example.com", encoder.encode(PASSWORD)));
        ownerId = owner.getId();
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Other Customer",
                "board.other@example.com", encoder.encode(PASSWORD)));
        UserAccount designer = UserAccount.newInteriorDesigner(null, "Designer",
                "board.designer@example.com", encoder.encode(PASSWORD));
        designer.replacePasswordHash(encoder.encode(PASSWORD));
        designer = accounts.saveAndFlush(designer);
        ProjectCategory category = categories.saveAndFlush(new ProjectCategory("Moodboard Living"));
        InteriorDesignProject project = projects.saveAndFlush(new InteriorDesignProject(designer, category,
                "Warm Japandi", "An interior inspiration project", ProjectStatus.COMPLETED));
        projectId = project.getId();
        photoId = photos.saveAndFlush(new ProjectImage(project, "moodboard-test-storage-key.png",
                "image/png", 120L, 0)).getId();
        jdbc.update("INSERT INTO customer_project_favourites(customer_id, project_id) VALUES (?, ?)", ownerId, projectId);
    }

    @AfterEach
    void cleanup() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        jdbc.update("DELETE FROM user_accounts");
        jdbc.update("DELETE FROM project_categories");
    }

    record LoggedIn(MockHttpSession session, String header, String token) {}

    private LoggedIn login(String email) throws Exception {
        MvcResult csrfResult = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(csrfResult.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        MvcResult loggedIn = mvc.perform(post("/api/login").session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        session = (MockHttpSession) loggedIn.getRequest().getSession(false);
        MvcResult refreshed = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode csrf = json.readTree(refreshed.getResponse().getContentAsString());
        return new LoggedIn(session, csrf.get("headerName").asText(), csrf.get("token").asText());
    }

    private JsonNode create(LoggedIn user) throws Exception {
        MvcResult result = mvc.perform(post(ROOT).session(user.session())
                .header(user.header(), user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Warm Japandi home\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Warm Japandi home"))
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private Map<String, Object> layout(long version, Long imageId) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("version", version);
        values.put("title", "New interior concept");
        values.put("backgroundColor", "#FAFAFA");
        values.put("elements", List.of(
                Map.of("kind", "PHOTO", "photoId", imageId, "sourceLabel", "Warm Japandi",
                        "x", 40, "y", 40, "width", 350, "height", 260),
                Map.of("kind", "TEXT", "text", "Soft natural materials", "color", "#365B82",
                        "fontSize", 28, "x", 420, "y", 80, "width", 270, "height", 120),
                Map.of("kind", "SWATCH", "color", "#C8B99F",
                        "x", 550, "y", 360, "width", 100, "height", 90)));
        return values;
    }

    @Test
    void createSaveReloadReorderAndDeleteArePersistentAndPrivate() throws Exception {
        LoggedIn owner = login("board.owner@example.com");
        LoggedIn other = login("board.other@example.com");
        JsonNode board = create(owner);
        long id = board.get("id").asLong();
        long initialVersion = board.get("version").asLong();
        mvc.perform(get(ROOT).session(owner.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Warm Japandi home"));
        mvc.perform(get(ROOT + "/" + id).session(other.session())).andExpect(status().isNotFound());
        mvc.perform(put(ROOT + "/" + id).session(owner.session())
                .header(owner.header(), owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(layout(initialVersion, photoId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements.length()").value(3))
                .andExpect(jsonPath("$.elements[0].photoUrl")
                        .value("/api/projects/" + projectId + "/images/" + photoId))
                .andExpect(jsonPath("$.elements[1].text").value("Soft natural materials"));
        mvc.perform(get(ROOT + "/" + id).session(owner.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backgroundColor").value("#FAFAFA"))
                .andExpect(jsonPath("$.elements[2].kind").value("SWATCH"));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM customer_moodboard_elements", Integer.class));
        mvc.perform(put(ROOT + "/" + id).session(other.session())
                .header(other.header(), other.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(layout(initialVersion, photoId))))
                .andExpect(status().isNotFound());
        mvc.perform(delete(ROOT + "/" + id).session(other.session())
                .header(other.header(), other.token())).andExpect(status().isNotFound());
        mvc.perform(delete(ROOT + "/" + id).session(owner.session())
                .header(owner.header(), owner.token())).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM customer_moodboard_elements", Integer.class));
    }

    @Test
    void photoPickerReadsFavouritesWithoutRecordingViewsAndPreservesPinnedAfterUnsaving() throws Exception {
        LoggedIn owner = login("board.owner@example.com");
        LoggedIn other = login("board.other@example.com");
        mvc.perform(get(ROOT + "/photos").session(owner.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].photos[0].id").value(photoId.intValue()));
        mvc.perform(get(ROOT + "/photos").session(other.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM project_view_counts", Integer.class));
        long boardId = create(owner).get("id").asLong();
        mvc.perform(put(ROOT + "/" + boardId).session(owner.session())
                .header(owner.header(), owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(layout(0, photoId))))
                .andExpect(status().isOk());
        jdbc.update("DELETE FROM customer_project_favourites WHERE customer_id = ?", ownerId);
        mvc.perform(get(ROOT + "/photos").session(owner.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(ROOT + "/" + boardId).session(owner.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.elements[0].unavailable").value(false));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM project_view_counts", Integer.class));
    }

    @Test
    void wrongRoleCsrfStaleVersionAndMalformedPositionsAreRejected() throws Exception {
        LoggedIn owner = login("board.owner@example.com");
        LoggedIn designer = login("board.designer@example.com");
        mvc.perform(get(ROOT).session(designer.session())).andExpect(status().isForbidden());
        mvc.perform(post(ROOT).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Denied\"}")).andExpect(status().isForbidden());
        mvc.perform(post(ROOT).session(owner.session()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Denied\"}")).andExpect(status().isForbidden());
        JsonNode board = create(owner);
        long id = board.get("id").asLong();
        mvc.perform(put(ROOT + "/" + id).session(owner.session())
                .header(owner.header(), owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(layout(0, photoId)))).andExpect(status().isOk());
        mvc.perform(put(ROOT + "/" + id).session(owner.session())
                .header(owner.header(), owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(layout(0, photoId)))).andExpect(status().isConflict());
        var invalid = layout(1, photoId);
        @SuppressWarnings("unchecked")
        var elements = (List<Map<String, Object>>) invalid.get("elements");
        var wrong = new LinkedHashMap<>(elements.get(0));
        wrong.put("x", 850);
        invalid.put("elements", List.of(wrong));
        mvc.perform(put(ROOT + "/" + id).session(owner.session())
                .header(owner.header(), owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
    }

    @Test
    void deletedPhotoShowsUnavailablePlaceholderRatherThanBreakingBoard() throws Exception {
        LoggedIn owner = login("board.owner@example.com");
        long boardId = create(owner).get("id").asLong();
        mvc.perform(put(ROOT + "/" + boardId).session(owner.session())
                .header(owner.header(), owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(layout(0, photoId)))).andExpect(status().isOk());
        jdbc.update("DELETE FROM project_images WHERE id = ?", photoId);
        mvc.perform(get(ROOT + "/" + boardId).session(owner.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements[0].unavailable").value(true))
                .andExpect(jsonPath("$.elements[0].photoUrl").isEmpty())
                .andExpect(jsonPath("$.elements[0].sourceLabel").value("Warm Japandi"));
    }
}
