package csit314.renovation.portfolio.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import csit314.renovation.portfolio.entity.ProjectStatus;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectDeletionIntegrationTest {
    private static final String PASSWORD = "DesignerSecure123!";
    private static final Path IMAGE_DIR = Path.of(System.getProperty("java.io.tmpdir"),
            "csit314-project-delete-test-" + UUID.randomUUID());

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

    private ProjectCategory category;
    private InteriorDesignProject project;
    private InteriorDesignProject otherProject;

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
        category = categories.saveAndFlush(new ProjectCategory("Residential"));
        project = projects.saveAndFlush(new InteriorDesignProject(first, category,
                "Original title", "First portfolio", ProjectStatus.CURRENT));
        otherProject = projects.saveAndFlush(new InteriorDesignProject(second, category,
                "Other designer", "Private project", ProjectStatus.COMPLETED));
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

    private MockMultipartFile validPng() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("images", "photo.png", "image/png", output.toByteArray());
    }

    @Test
    void deletesOwnProjectAndStoredPhotosButPreservesAnotherDesignerProject() throws Exception {
        SessionCsrf owner = login("first@example.com", PASSWORD);
        MvcResult created = mvc.perform(multipart("/api/designer/projects")
                        .file(new MockMultipartFile("project", "project.json", "application/json",
                                json.writeValueAsBytes(Map.of("title", "Photo project",
                                        "description", "Photos", "categoryId", category.getId(),
                                        "status", "CURRENT"))))
                        .file(validPng()).session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isCreated()).andReturn();
        JsonNode uploaded = json.readTree(created.getResponse().getContentAsString());
        long createdId = uploaded.get("id").asLong();
        long imageId = uploaded.get("images").get(0).get("id").asLong();
        String storedKey = images.findById(imageId).orElseThrow().getStorageKey();
        assertTrue(Files.exists(IMAGE_DIR.resolve(storedKey)));

        mvc.perform(delete("/api/designer/projects/{id}", createdId)
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isNoContent());

        assertFalse(projects.existsById(createdId));
        assertFalse(images.existsById(imageId));
        assertFalse(Files.exists(IMAGE_DIR.resolve(storedKey)));
        assertTrue(projects.existsById(project.getId()));
        assertTrue(projects.existsById(otherProject.getId()));
        assertEquals(2, projects.count());
        mvc.perform(get("/api/designer/projects").session(owner.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Original title"));
        mvc.perform(get("/api/designer/projects/{id}/images/{imageId}", createdId, imageId)
                        .session(owner.session())).andExpect(status().isNotFound());
    }

    @Test
    void deniesDeletingOtherDesignerProjectAndUnknownOrAlreadyDeletedProject() throws Exception {
        SessionCsrf owner = login("first@example.com", PASSWORD);
        SessionCsrf other = login("second@example.com", PASSWORD);
        mvc.perform(delete("/api/designer/projects/{id}", otherProject.getId())
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/designer/projects/{id}", Long.MAX_VALUE)
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isNotFound());
        assertTrue(projects.existsById(otherProject.getId()));

        mvc.perform(delete("/api/designer/projects/{id}", project.getId())
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/designer/projects/{id}", project.getId())
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isNotFound());
        assertTrue(projects.existsById(otherProject.getId()));
        mvc.perform(delete("/api/designer/projects/{id}", otherProject.getId())
                        .session(other.session()).header(other.header(), other.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void requiresSessionCsrfDesignerRoleAndCompletedPasswordChange() throws Exception {
        SessionCsrf owner = login("first@example.com", PASSWORD);
        mvc.perform(delete("/api/designer/projects/{id}", project.getId())
                        .session(owner.session())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/designer/projects/{id}", project.getId()))
                .andExpect(status().isForbidden());
        SessionCsrf customer = login("customer@example.com", "CustomerSecure123!");
        mvc.perform(delete("/api/designer/projects/{id}", project.getId())
                        .session(customer.session()).header(customer.header(), customer.token()))
                .andExpect(status().isForbidden());
        SessionCsrf pending = login("pending@example.com", PASSWORD);
        mvc.perform(delete("/api/designer/projects/{id}", project.getId())
                        .session(pending.session()).header(pending.header(), pending.token()))
                .andExpect(status().isForbidden());
        assertTrue(projects.existsById(project.getId()));
    }
}
