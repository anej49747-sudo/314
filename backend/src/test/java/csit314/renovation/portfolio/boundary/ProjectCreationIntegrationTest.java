package csit314.renovation.portfolio.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import csit314.renovation.portfolio.repository.ProjectImageRepository;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Map;
import java.util.UUID;
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
class ProjectCreationIntegrationTest {
    private static final String ENDPOINT = "/api/designer/projects";
    private static final String PASSWORD = "DesignerSecure123!";
    private static final Path IMAGE_DIR = Path.of(System.getProperty("java.io.tmpdir"),
            "csit314-portfolio-test-" + UUID.randomUUID());

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

    @BeforeEach
    void prepare() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        images.deleteAll();
        projects.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();
        createDesigner("first@example.com", true);
        createDesigner("second@example.com", true);
        createDesigner("pending@example.com", false);
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer", "customer@example.com",
                encoder.encode("CustomerSecure123!")));
        category = categories.saveAndFlush(new ProjectCategory("Residential"));
    }

    @AfterEach
    void cleanup() throws Exception {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        images.deleteAll();
        projects.deleteAll();
        categories.deleteAll();
        accounts.deleteAll();
        if (Files.isDirectory(IMAGE_DIR)) {
            try (var files = Files.list(IMAGE_DIR)) {
                for (Path file : files.toList()) Files.deleteIfExists(file);
            }
            Files.deleteIfExists(IMAGE_DIR);
        }
    }

    private void createDesigner(String email, boolean passwordChanged) {
        UserAccount designer = UserAccount.newInteriorDesigner(null, "Designer", email,
                encoder.encode(PASSWORD));
        if (passwordChanged) designer.replacePasswordHash(encoder.encode(PASSWORD));
        accounts.saveAndFlush(designer);
    }

    private record SessionCsrf(MockHttpSession session, String header, String token) { }

    private SessionCsrf login(String email, String password) throws Exception {
        MvcResult firstCsrf = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode csrf = json.readTree(firstCsrf.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) firstCsrf.getRequest().getSession(false);
        MvcResult result = mvc.perform(post("/api/login").session(session)
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        session = (MockHttpSession) result.getRequest().getSession(false);
        MvcResult refreshed = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        csrf = json.readTree(refreshed.getResponse().getContentAsString());
        return new SessionCsrf(session, csrf.get("headerName").asText(), csrf.get("token").asText());
    }

    private MockMultipartHttpServletRequestBuilder request(long categoryId) throws Exception {
        return multipart(ENDPOINT).file(new MockMultipartFile("project", "project.json", "application/json",
                json.writeValueAsBytes(Map.of("title", "  Kitchen redesign  ",
                        "description", "  New cabinetry  ", "categoryId", categoryId,
                        "status", "COMPLETED"))));
    }

    private MockMultipartFile validPng() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("images", "kitchen.png", "image/png", output.toByteArray());
    }

    @Test
    void createsProjectWithCategoryAndMakesItVisibleInOwnPortfolio() throws Exception {
        SessionCsrf designer = login("first@example.com", PASSWORD);
        MvcResult created = mvc.perform(request(category.getId()).session(designer.session())
                        .header(designer.header(), designer.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Kitchen redesign"))
                .andExpect(jsonPath("$.description").value("New cabinetry"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.categoryName").value("Residential"))
                .andExpect(jsonPath("$.images.length()").value(0))
                .andReturn();
        assertEquals(1, projects.count());
        assertEquals(0, images.count());
        mvc.perform(get(ENDPOINT).session(designer.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(ENDPOINT).session(login("second@example.com", PASSWORD).session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        assertEquals("Kitchen redesign", json.readTree(created.getResponse().getContentAsString())
                .get("title").asText());
    }

    @Test
    void storesAndServesImageOnlyToItsOwner() throws Exception {
        SessionCsrf owner = login("first@example.com", PASSWORD);
        MvcResult result = mvc.perform(request(category.getId()).file(validPng())
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.images.length()").value(1))
                .andReturn();
        String url = json.readTree(result.getResponse().getContentAsString())
                .get("images").get(0).get("url").asText();
        assertEquals(1, images.count());
        mvc.perform(get(url).session(owner.session())).andExpect(status().isOk());
        mvc.perform(get(url).session(login("second@example.com", PASSWORD).session()))
                .andExpect(status().isNotFound());
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnknownCategoryAndInvalidImageWithoutSavingProject() throws Exception {
        SessionCsrf designer = login("first@example.com", PASSWORD);
        mvc.perform(request(Long.MAX_VALUE).session(designer.session())
                        .header(designer.header(), designer.token()))
                .andExpect(status().isBadRequest());
        mvc.perform(request(category.getId()).file(new MockMultipartFile(
                        "images", "fake.png", "image/png", "not a real image".getBytes()))
                        .session(designer.session()).header(designer.header(), designer.token()))
                .andExpect(status().isBadRequest());
        assertEquals(0, projects.count());
        assertEquals(0, images.count());
    }

    @Test
    void requiresSessionRolePasswordChangeAndCsrf() throws Exception {
        mvc.perform(request(category.getId())).andExpect(status().isForbidden());
        SessionCsrf owner = login("first@example.com", PASSWORD);
        mvc.perform(request(category.getId()).session(owner.session()))
                .andExpect(status().isForbidden());
        SessionCsrf customer = login("customer@example.com", "CustomerSecure123!");
        mvc.perform(request(category.getId()).session(customer.session())
                        .header(customer.header(), customer.token()))
                .andExpect(status().isForbidden());
        SessionCsrf pending = login("pending@example.com", PASSWORD);
        mvc.perform(request(category.getId()).session(pending.session())
                        .header(pending.header(), pending.token()))
                .andExpect(status().isForbidden());
        assertEquals(0, projects.count());
    }

    @Test
    void rejectsBadFieldValues() throws Exception {
        SessionCsrf owner = login("first@example.com", PASSWORD);
        mvc.perform(multipart(ENDPOINT).file(new MockMultipartFile("project", "project.json", "application/json",
                        json.writeValueAsBytes(Map.of("title", " ", "description", " ",
                                "categoryId", category.getId(), "status", "CURRENT"))))
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
        assertEquals(0, projects.count());
    }
}
