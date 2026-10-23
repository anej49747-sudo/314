package csit314.renovation.article.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import csit314.renovation.article.repository.RenovationArticleRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
class RenovationArticleIntegrationTest {
    private static final Path COVER_DIR = Path.of(System.getProperty("java.io.tmpdir"),
            "csit314-articles-test-" + UUID.randomUUID());
    private static final String CONTENT = "Planning renovations starts by writing down the work each room needs. "
            + "Discuss the scope, budget and timeline with a qualified designer, then compare your options carefully.";

    @DynamicPropertySource
    static void configureStorage(DynamicPropertyRegistry registry) {
        registry.add("app.project-images.directory", () -> COVER_DIR.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired RenovationArticleRepository articles;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void prepare() {
        clearDatabase();
        accounts.saveAndFlush(UserAccount.newPlatformManager("Articles Manager", "articlesmanager@example.com",
                encoder.encode("ManagerSecure123!")));
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Article Reader", "articlescustomer@example.com",
                encoder.encode("CustomerSecure123!")));
    }

    @AfterEach
    void cleanup() throws IOException {
        clearDatabase();
        if (Files.exists(COVER_DIR)) {
            try (var paths = Files.walk(COVER_DIR)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private void clearDatabase() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        articles.deleteAll();
        accounts.deleteAll();
    }

    private record SessionCsrf(MockHttpSession session, String header, String token) { }

    private SessionCsrf csrf(MockHttpSession session) throws Exception {
        var request = get("/api/auth/csrf");
        MvcResult result = mvc.perform(session == null ? request : request.session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = json.readTree(result.getResponse().getContentAsString());
        return new SessionCsrf((MockHttpSession) result.getRequest().getSession(false),
                data.get("headerName").asText(), data.get("token").asText());
    }

    private SessionCsrf login(String email, String password) throws Exception {
        SessionCsrf before = csrf(null);
        MvcResult result = mvc.perform(post("/api/login").session(before.session())
                .header(before.header(), before.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return csrf((MockHttpSession) result.getRequest().getSession(false));
    }

    private MockMultipartFile article(String title, String summary, String content) throws Exception {
        return new MockMultipartFile("article", "article.json", "application/json",
                json.writeValueAsBytes(Map.of("title", title, "summary", summary, "content", content)));
    }

    private MockMultipartFile validCover() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return new MockMultipartFile("cover", "cover.png", "image/png", bytes.toByteArray());
    }

    @Test
    void managerPublishesWithCoverAndGuestsCanReadArticleAndImage() throws Exception {
        SessionCsrf manager = login("articlesmanager@example.com", "ManagerSecure123!");
        MvcResult result = mvc.perform(multipart("/api/manager/articles")
                .file(article("Renovation budget planning", "Understand your renovation budget before starting.", CONTENT))
                .file(validCover()).session(manager.session()).header(manager.header(), manager.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Renovation budget planning"))
                .andExpect(jsonPath("$.coverUrl").isNotEmpty())
                .andExpect(jsonPath("$.publishedAt").isNotEmpty()).andReturn();
        long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/articles")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/articles/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(CONTENT));
        mvc.perform(get("/api/articles/{id}/cover", id)).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentType(MediaType.IMAGE_PNG));
        assertEquals(1, articles.count());
        try (var files = Files.list(COVER_DIR)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void publishingWithoutPhotoWorksAndMissingPhotoReturns404() throws Exception {
        SessionCsrf manager = login("articlesmanager@example.com", "ManagerSecure123!");
        MvcResult result = mvc.perform(multipart("/api/manager/articles")
                .file(article("Renovation budget planning", "Understand your renovation budget before starting.", CONTENT))
                .session(manager.session()).header(manager.header(), manager.token()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.coverUrl").isEmpty()).andReturn();
        long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/articles/{id}/cover", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/articles/{id}", 99999999)).andExpect(status().isNotFound());
        assertEquals(1, articles.count());
    }

    @Test
    void onlyManagerWithValidCsrfCanPublish() throws Exception {
        var body = article("Renovation budget planning", "Understand your renovation budget before starting.", CONTENT);
        SessionCsrf guest = csrf(null);
        mvc.perform(multipart("/api/manager/articles").file(body).session(guest.session())
                .header(guest.header(), guest.token())).andExpect(status().isUnauthorized());
        SessionCsrf customer = login("articlescustomer@example.com", "CustomerSecure123!");
        mvc.perform(multipart("/api/manager/articles").file(body).session(customer.session())
                .header(customer.header(), customer.token())).andExpect(status().isForbidden());
        SessionCsrf manager = login("articlesmanager@example.com", "ManagerSecure123!");
        mvc.perform(multipart("/api/manager/articles").file(body).session(manager.session()))
                .andExpect(status().isForbidden());
        assertEquals(0, articles.count());
    }

    @Test
    void invalidTextOrFileCannotPublishOrWriteCover() throws Exception {
        SessionCsrf manager = login("articlesmanager@example.com", "ManagerSecure123!");
        mvc.perform(multipart("/api/manager/articles").file(article("No", "Too short", "Too short"))
                .session(manager.session()).header(manager.header(), manager.token()))
                .andExpect(status().isBadRequest());
        var fakeImage = new MockMultipartFile("cover", "not-an-image.png", "image/png",
                "not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/manager/articles")
                .file(article("Renovation budget planning", "Understand your renovation budget before starting.", CONTENT))
                .file(fakeImage).session(manager.session()).header(manager.header(), manager.token()))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/manager/articles")
                .file(article("Renovation budget planning", "Understand your renovation budget before starting.", CONTENT))
                .file(new MockMultipartFile("cover", "oversized.png", "image/png", new byte[5 * 1024 * 1024 + 1]))
                .session(manager.session()).header(manager.header(), manager.token()))
                .andExpect(status().is4xxClientError());
        assertEquals(0, articles.count());
        if (Files.exists(COVER_DIR)) {
            try (var files = Files.list(COVER_DIR)) {
                assertTrue(files.findAny().isEmpty());
            }
        }
    }
}
