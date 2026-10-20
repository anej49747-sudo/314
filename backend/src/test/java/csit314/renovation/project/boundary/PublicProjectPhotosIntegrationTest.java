package csit314.renovation.project.boundary;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.category.entity.ProjectCategory;
import csit314.renovation.category.repository.ProjectCategoryRepository;
import csit314.renovation.portfolio.control.ProjectImageStorage;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectImage;
import csit314.renovation.portfolio.entity.ProjectStatus;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicProjectPhotosIntegrationTest {
    private static final Path IMAGE_DIR = Path.of(System.getProperty("java.io.tmpdir"),
            "csit314-public-photos-test-" + UUID.randomUUID());

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
    @Autowired ProjectImageStorage storage;
    @Autowired PasswordEncoder encoder;

    private final List<String> storedKeys = new ArrayList<>();
    private UserAccount designer;
    private ProjectCategory category;

    @BeforeEach
    void prepare() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        images.deleteAll();
        projects.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();
        designer = accounts.saveAndFlush(UserAccount.newInteriorDesigner(null,
                "Taylor", "photos@example.com", encoder.encode("TempPassword123!")));
        category = categories.saveAndFlush(new ProjectCategory("Residential"));
    }

    @AfterEach
    void cleanup() throws Exception {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        images.deleteAll();
        projects.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();
        for (String key : storedKeys) storage.deleteQuietly(key);
        if (Files.isDirectory(IMAGE_DIR)) Files.deleteIfExists(IMAGE_DIR);
    }

    private InteriorDesignProject project(String title) {
        return projects.saveAndFlush(new InteriorDesignProject(designer, category,
                title, "Sample project details", ProjectStatus.COMPLETED));
    }

    private static byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private ProjectImage addPhoto(InteriorDesignProject project, byte[] bytes, int order) {
        ProjectImageStorage.StoredImage stored = storage.store(
                new ProjectImageStorage.ValidatedImage(bytes, "image/png", ".png"));
        storedKeys.add(stored.key());
        return images.saveAndFlush(new ProjectImage(project, stored.key(),
                stored.contentType(), stored.sizeBytes(), order));
    }

    private static String photoUrl(long projectId, long photoId) {
        return "/api/projects/" + projectId + "/images/" + photoId;
    }

    @Test
    void returnsOrderedPublicPhotoMetadataAndActualImageBytes() throws Exception {
        InteriorDesignProject project = project("Open-plan apartment");
        byte[] bytes = pngBytes();
        ProjectImage later = addPhoto(project, bytes, 1);
        ProjectImage first = addPhoto(project, bytes, 0);
        String firstUrl = photoUrl(project.getId(), first.getId());

        mvc.perform(get("/api/projects/{id}", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].id").value(first.getId().intValue()))
                .andExpect(jsonPath("$.images[0].url").value(firstUrl))
                .andExpect(jsonPath("$.images[1].id").value(later.getId().intValue()))
                .andExpect(jsonPath("$.images[0].storageKey").doesNotExist())
                .andExpect(jsonPath("$.images[0].contentType").doesNotExist())
                .andExpect(jsonPath("$.images[0].sizeBytes").doesNotExist());

        MvcResult result = mvc.perform(get(firstUrl))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn();
        assertArrayEquals(bytes, result.getResponse().getContentAsByteArray());
        // A public photo URL must not accidentally open the designer-only URL.
        mvc.perform(get("/api/designer/projects/{id}/images/{imageId}",
                        project.getId(), first.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsEmptyPhotoListWhenProjectHasNoImages() throws Exception {
        InteriorDesignProject project = project("Photo-free project");
        mvc.perform(get("/api/projects/{id}", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(0));
    }

    @Test
    void doesNotLeakPhotosFromInactiveDesigner() throws Exception {
        InteriorDesignProject project = project("Private project");
        ProjectImage photo = addPhoto(project, pngBytes(), 0);
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", designer.getId());
        mvc.perform(get("/api/projects/{id}", project.getId()))
                .andExpect(status().isNotFound());
        mvc.perform(get(photoUrl(project.getId(), photo.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsWrongProjectAndUnknownPhotoOrProject() throws Exception {
        InteriorDesignProject first = project("Project one");
        InteriorDesignProject second = project("Project two");
        ProjectImage image = addPhoto(first, pngBytes(), 0);
        mvc.perform(get(photoUrl(second.getId(), image.getId())))
                .andExpect(status().isNotFound());
        mvc.perform(get(photoUrl(first.getId(), Long.MAX_VALUE)))
                .andExpect(status().isNotFound());
        mvc.perform(get(photoUrl(Long.MAX_VALUE, image.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void designerPendingInitialPasswordChangeCanViewPublicPhotos() throws Exception {
        InteriorDesignProject project = project("Public for designers too");
        ProjectImage photo = addPhoto(project, pngBytes(), 0);
        MvcResult csrfResult = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode csrf = json.readTree(csrfResult.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        MvcResult login = mvc.perform(post("/api/login").session(session)
                        .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "photos@example.com",
                                "password", "TempPassword123!"))))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession loggedIn = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get(photoUrl(project.getId(), photo.getId())).session(loggedIn))
                .andExpect(status().isOk());
    }
}
