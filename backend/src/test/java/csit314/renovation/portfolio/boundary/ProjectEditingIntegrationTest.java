package csit314.renovation.portfolio.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectImage;
import csit314.renovation.portfolio.entity.ProjectStatus;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectEditingIntegrationTest {
    private static final String PASSWORD = "DesignerSecure123!";
    private static final Path IMAGE_DIR = Path.of(System.getProperty("java.io.tmpdir"),
            "csit314-project-edit-test-" + UUID.randomUUID());

    @DynamicPropertySource
    static void imageDirectory(DynamicPropertyRegistry registry) {
        registry.add("app.project-images.directory", () -> IMAGE_DIR.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired ProjectCategoryRepository categories;
    @Autowired InteriorDesignProjectRepository projects;
    @Autowired ProjectImageRepository images;
    @Autowired PasswordEncoder encoder;

    private InteriorDesignProject project;
    private InteriorDesignProject otherProject;
    private ProjectCategory anotherCategory;

    @BeforeEach
    void prepare() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        images.deleteAll();
        projects.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();
        UserAccount first = createDesigner("first@example.com", true);
        UserAccount second = createDesigner("second@example.com", true);
        createDesigner("pending@example.com", false);
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer", "customer@example.com",
                encoder.encode("CustomerSecure123!")));
        ProjectCategory category = categories.saveAndFlush(new ProjectCategory("Residential"));
        anotherCategory = categories.saveAndFlush(new ProjectCategory("Commercial"));
        project = projects.saveAndFlush(new InteriorDesignProject(first, category,
                "Original title", "Original description", ProjectStatus.CURRENT));
        otherProject = projects.saveAndFlush(new InteriorDesignProject(second, category,
                "Other designer", "Private project", ProjectStatus.CURRENT));
    }

    @AfterEach
    void cleanup() throws Exception {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        images.deleteAll();
        projects.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();
        if (Files.isDirectory(IMAGE_DIR)) {
            try (var entries = Files.list(IMAGE_DIR)) {
                for (Path file : entries.toList()) Files.deleteIfExists(file);
            }
            Files.deleteIfExists(IMAGE_DIR);
        }
    }

    private UserAccount createDesigner(String email, boolean passwordChanged) {
        UserAccount designer = UserAccount.newInteriorDesigner(null, "Designer", email, encoder.encode(PASSWORD));
        if (passwordChanged) designer.replacePasswordHash(encoder.encode(PASSWORD));
        return accounts.saveAndFlush(designer);
    }

    private record SessionCsrf(MockHttpSession session, String header, String token) { }

    private SessionCsrf login(String email, String password) throws Exception {
        MvcResult response = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode csrf = json.readTree(response.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) response.getRequest().getSession(false);
        MvcResult loggedIn = mvc.perform(post("/api/login").session(session)
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        session = (MockHttpSession) loggedIn.getRequest().getSession(false);
        response = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        csrf = json.readTree(response.getResponse().getContentAsString());
        return new SessionCsrf(session, csrf.get("headerName").asText(), csrf.get("token").asText());
    }

    private MockMultipartHttpServletRequestBuilder update(Long projectId, Object keepImageIds,
            Long categoryId) throws Exception {
        MockMultipartHttpServletRequestBuilder builder =
                multipart("/api/designer/projects/{id}", projectId);
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });
        builder.file(new MockMultipartFile("project", "project.json", "application/json",
                json.writeValueAsBytes(Map.of("title", "  Updated title  ",
                        "description", "  Updated description  ",
                        "categoryId", categoryId, "status", "COMPLETED",
                        "keepImageIds", keepImageIds))));
        return builder;
    }

    private MockMultipartFile validPng() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("images", "updated.png", "image/png", output.toByteArray());
    }

    @Test
    void editsOwnProjectWithoutCreatingADuplicate() throws Exception {
        SessionCsrf owner = login("first@example.com", PASSWORD);
        mvc.perform(update(project.getId(), List.of(), anotherCategory.getId())
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.categoryName").value("Commercial"));
        assertEquals(2, projects.count());
        assertEquals("Other designer", projects.findById(otherProject.getId()).orElseThrow().getTitle());
        assertEquals("Updated title", projects.findById(project.getId()).orElseThrow().getTitle());
    }

    @Test
    void keepsPhotosRemovesChosenPhotosAndAddsNewPhoto() throws Exception {
        SessionCsrf owner = login("first@example.com", PASSWORD);
        // Create photos through the existing creation API so the storage keys and files are real.
        MvcResult created = mvc.perform(multipart("/api/designer/projects")
                        .file(new MockMultipartFile("project", "project.json", "application/json",
                                json.writeValueAsBytes(Map.of("title", "Photos project",
                                        "description", "Gallery", "categoryId", anotherCategory.getId(),
                                        "status", "CURRENT"))))
                        .file(validPng()).file(validPng())
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isCreated()).andReturn();
        JsonNode original = json.readTree(created.getResponse().getContentAsString());
        long newProjectId = original.get("id").asLong();
        long retainedId = original.get("images").get(0).get("id").asLong();
        long removedId = original.get("images").get(1).get("id").asLong();
        String oldKey = images.findById(removedId).orElseThrow().getStorageKey();
        assertTrue(Files.exists(IMAGE_DIR.resolve(oldKey)));
        mvc.perform(update(newProjectId, List.of(retainedId), anotherCategory.getId())
                        .file(validPng()).session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].id").value((int) retainedId));
        assertFalse(images.existsById(removedId));
        assertFalse(Files.exists(IMAGE_DIR.resolve(oldKey)));
        mvc.perform(get("/api/designer/projects/{id}/images/{imageId}", newProjectId, removedId)
                        .session(owner.session())).andExpect(status().isNotFound());
    }

    @Test
    void rejectsOtherDesignerUnknownProjectBadImageReferencesAndOversizedGallery() throws Exception {
        SessionCsrf owner = login("first@example.com", PASSWORD);
        SessionCsrf other = login("second@example.com", PASSWORD);
        mvc.perform(update(project.getId(), List.of(), anotherCategory.getId())
                        .session(other.session()).header(other.header(), other.token()))
                .andExpect(status().isNotFound());
        mvc.perform(update(Long.MAX_VALUE, List.of(), anotherCategory.getId())
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isNotFound());
        mvc.perform(update(project.getId(), List.of(999999L), anotherCategory.getId())
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isBadRequest());
        ProjectImage foreign = images.saveAndFlush(new ProjectImage(otherProject,
                UUID.randomUUID() + ".png", "image/png", 10L, 0));
        mvc.perform(update(project.getId(), List.of(foreign.getId()), anotherCategory.getId())
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isBadRequest());
        mvc.perform(update(project.getId(), List.of(1L, 1L), anotherCategory.getId())
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isBadRequest());
        MockMultipartHttpServletRequestBuilder tooMany = update(project.getId(), List.of(), anotherCategory.getId());
        for (int i = 0; i < 7; i++) tooMany.file(validPng());
        mvc.perform(tooMany.session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isBadRequest());
        assertEquals("Original title", projects.findById(project.getId()).orElseThrow().getTitle());
    }

    @Test
    void requiresCsrfSessionDesignerRoleAndChangedPassword() throws Exception {
        SessionCsrf owner = login("first@example.com", PASSWORD);
        mvc.perform(update(project.getId(), List.of(), anotherCategory.getId()).session(owner.session()))
                .andExpect(status().isForbidden());
        mvc.perform(update(project.getId(), List.of(), anotherCategory.getId()))
                .andExpect(status().isForbidden());
        SessionCsrf customer = login("customer@example.com", "CustomerSecure123!");
        mvc.perform(update(project.getId(), List.of(), anotherCategory.getId())
                        .session(customer.session()).header(customer.header(), customer.token()))
                .andExpect(status().isForbidden());
        SessionCsrf pending = login("pending@example.com", PASSWORD);
        mvc.perform(update(project.getId(), List.of(), anotherCategory.getId())
                        .session(pending.session()).header(pending.header(), pending.token()))
                .andExpect(status().isForbidden());
    }
}
