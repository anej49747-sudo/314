package csit314.renovation.design.boundary;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerDesignDraftIntegrationTest {
    private static final String ROOT = "/api/customer/designs";
    private static final String PASSWORD = "DraftsTest123!";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordEncoder encoder;

    @BeforeEach void setup() {
        cleanup();
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Draft Owner", "draft.owner@example.com", encoder.encode(PASSWORD)));
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Another Customer", "draft.other@example.com", encoder.encode(PASSWORD)));
        accounts.saveAndFlush(UserAccount.newUserAdmin("Draft Admin", "draft.admin@example.com", encoder.encode(PASSWORD)));
    }

    @AfterEach void cleanup() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        jdbc.update("DELETE FROM user_accounts");
        jdbc.update("DELETE FROM project_categories");
    }

    record Session(MockHttpSession session, String header, String token) {}

    private Session login(String email) throws Exception {
        MvcResult seed = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode csrf = mapper.readTree(seed.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) seed.getRequest().getSession(false);
        MvcResult authenticated = mvc.perform(post("/api/login").session(session)
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        session = (MockHttpSession) authenticated.getRequest().getSession(false);
        MvcResult refreshed = mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
        JsonNode credentials = mapper.readTree(refreshed.getResponse().getContentAsString());
        return new Session(session, credentials.get("headerName").asText(), credentials.get("token").asText());
    }

    private JsonNode create(Session user, String template) throws Exception {
        MvcResult result = mvc.perform(post(ROOT).session(user.session())
                .header(user.header(), user.token()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("title", "My apartment concept", "floorWidthCm", 1200,
                        "floorHeightCm", 800, "template", template))))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private Map<String, Object> layout(long version) {
        return Map.of("version", version, "title", "Revised layout", "floorWidthCm", 1200,
                "floorHeightCm", 800, "elements", List.of(
                        Map.of("kind", "ROOM", "name", "Kitchen", "xCm", 0, "yCm", 0,
                                "widthCm", 600, "heightCm", 400, "rotation", 0),
                        Map.of("kind", "ROOM", "name", "Bedroom", "xCm", 500, "yCm", 300,
                                "widthCm", 600, "heightCm", 450, "rotation", 0),
                        Map.of("kind", "FURNITURE", "name", "Bed", "xCm", 560, "yCm", 350,
                                "widthCm", 210, "heightCm", 190, "rotation", 90)));
    }

    @Test void createEditReopenDeleteWithSpatialWarningsAndOwnership() throws Exception {
        Session owner = login("draft.owner@example.com");
        Session other = login("draft.other@example.com");
        JsonNode created = create(owner, "BLANK");
        long id = created.get("id").asLong();
        long version = created.get("version").asLong();
        mvc.perform(get(ROOT).session(owner.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("My apartment concept"));
        mvc.perform(get(ROOT + "/" + id).session(other.session())).andExpect(status().isNotFound());
        mvc.perform(put(ROOT + "/" + id).session(owner.session())
                .header(owner.header(), owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(layout(version))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roomOverlapCount").value(1))
                .andExpect(jsonPath("$.elements.length()").value(3));
        mvc.perform(get(ROOT + "/" + id).session(owner.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Revised layout"))
                .andExpect(jsonPath("$.elements[2].rotation").value(90));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM customer_design_elements", Integer.class));
        mvc.perform(delete(ROOT + "/" + id).session(other.session())
                .header(other.header(), other.token())).andExpect(status().isNotFound());
        mvc.perform(delete(ROOT + "/" + id).session(owner.session())
                .header(owner.header(), owner.token())).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM customer_design_elements", Integer.class));
    }

    @Test void sampleTemplateAndVersionConflicts() throws Exception {
        Session owner = login("draft.owner@example.com");
        JsonNode draft = create(owner, "SAMPLE");
        long id = draft.get("id").asLong();
        long version = draft.get("version").asLong();
        assertEquals(11, draft.get("elements").size());
        mvc.perform(put(ROOT + "/" + id).session(owner.session())
                .header(owner.header(), owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(layout(version))))
                .andExpect(status().isOk());
        mvc.perform(put(ROOT + "/" + id).session(owner.session())
                .header(owner.header(), owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(layout(version))))
                .andExpect(status().isConflict());
    }

    @Test void badGeometryAndTemplatesAreRejectedWithoutOverwritingSavedData() throws Exception {
        Session owner = login("draft.owner@example.com");
        JsonNode draft = create(owner, "BLANK");
        long id = draft.get("id").asLong();
        long version = draft.get("version").asLong();
        var invalid = Map.of("version", version, "title", "Bad geometry", "floorWidthCm", 1200,
                "floorHeightCm", 800, "elements", List.of(
                        Map.of("kind", "ROOM", "name", "Out of bounds", "xCm", 1100, "yCm", 0,
                                "widthCm", 500, "heightCm", 300, "rotation", 0)));
        mvc.perform(put(ROOT + "/" + id).session(owner.session())
                .header(owner.header(), owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
        mvc.perform(get(ROOT + "/" + id).session(owner.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.elements.length()").value(0));
        mvc.perform(post(ROOT).session(owner.session())
                .header(owner.header(), owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Wrong\",\"floorWidthCm\":1200,\"floorHeightCm\":800,\"template\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void csrfRolesAndAnonymousAccessAreRestricted() throws Exception {
        Session owner = login("draft.owner@example.com");
        Session admin = login("draft.admin@example.com");
        mvc.perform(get(ROOT)).andExpect(status().isUnauthorized());
        mvc.perform(get(ROOT).session(admin.session())).andExpect(status().isForbidden());
        mvc.perform(post(ROOT).session(owner.session()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"No CSRF\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(ROOT).session(admin.session()).header(admin.header(), admin.token())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
