package csit314.renovation.review.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import java.sql.Timestamp;
import java.time.Instant;
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
class PublicDesignerReviewIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordEncoder encoder;

    private Long designerId;
    private Long customerId;

    @BeforeEach
    void prepare() {
        cleanup();
        designerId = accounts.saveAndFlush(UserAccount.newInteriorDesigner(null, "Studio",
                "studio@example.com", encoder.encode("TestPassword123!"))).getId();
        customerId = accounts.saveAndFlush(UserAccount.newCustomer(null, "Private Customer",
                "private@example.com", encoder.encode("TestPassword123!"))).getId();
    }

    @AfterEach
    void cleanup() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        jdbc.update("DELETE FROM designer_reviews");
        jdbc.update("DELETE FROM user_accounts");
    }

    private String url() { return "/api/designers/" + designerId + "/reviews"; }

    private void addReview(int rating, String comment, String timestamp) {
        jdbc.update("INSERT INTO designer_reviews (designer_id, customer_id, rating, comment, created_at) "
                + "VALUES (?, ?, ?, ?, ?)", designerId, customerId, rating, comment,
                Timestamp.from(Instant.parse(timestamp)));
    }

    @Test
    void anonymousVisitorsCanSeeSummaryAndNewestThreeWithNoCustomerIdentity() throws Exception {
        addReview(1, "Oldest", "2026-10-18T08:00:00Z");
        addReview(5, "First five", "2026-10-19T08:00:00Z");
        addReview(5, null, "2026-10-20T08:00:00Z");
        addReview(3, "Newer", "2026-10-21T08:00:00Z");
        addReview(4, "Newest", "2026-10-22T08:00:00Z");

        String body = mvc.perform(get(url()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(5))
                .andExpect(jsonPath("$.averageRating").value(3.6))
                .andExpect(jsonPath("$.ratingDistribution['5']").value(2))
                .andExpect(jsonPath("$.ratingDistribution['4']").value(1))
                .andExpect(jsonPath("$.ratingDistribution['3']").value(1))
                .andExpect(jsonPath("$.ratingDistribution['2']").value(0))
                .andExpect(jsonPath("$.ratingDistribution['1']").value(1))
                .andExpect(jsonPath("$.reviews.length()").value(3))
                .andExpect(jsonPath("$.reviews[0].comment").value("Newest"))
                .andExpect(jsonPath("$.reviews[1].comment").value("Newer"))
                .andExpect(jsonPath("$.reviews[2].comment").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andReturn().getResponse().getContentAsString();
        JsonNode result = json.readTree(body);
        assertFalse(body.contains("Private Customer"));
        assertFalse(body.contains("private@example.com"));
        assertFalse(result.get("reviews").get(0).has("customerId"));
        assertFalse(result.get("reviews").get(0).has("designerId"));
        assertFalse(result.get("reviews").get(0).has("customer"));
        assertEquals("2026-10-22T08:00:00Z", result.get("reviews").get(0).get("createdAt").asText());
    }

    @Test
    void subsequentPageAndEmptyResponseWork() throws Exception {
        mvc.perform(get(url()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(0))
                .andExpect(jsonPath("$.averageRating").value(0))
                .andExpect(jsonPath("$.reviews.length()").value(0))
                .andExpect(jsonPath("$.hasMore").value(false));
        for (int i = 1; i <= 4; i++) addReview(i, "Review " + i,
                "2026-10-" + (10 + i) + "T08:00:00Z");
        mvc.perform(get(url() + "?page=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(4))
                .andExpect(jsonPath("$.reviews.length()").value(1))
                .andExpect(jsonPath("$.reviews[0].comment").value("Review 1"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void signedInDesignerPendingPasswordChangeCanStillReadPublicReviews() throws Exception {
        // Public profile features should remain readable during the mandatory password reset gate.
        MvcResult initial = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(initial.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) initial.getRequest().getSession(false);
        MvcResult login = mvc.perform(post("/api/login").session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "email", "studio@example.com", "password", "TestPassword123!"))))
                .andExpect(status().isOk()).andReturn();
        mvc.perform(get(url()).session((MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(0));
    }

    @Test
    void unknownInactiveOrCustomerAccountCannotBeRead() throws Exception {
        mvc.perform(get("/api/designers/999999999/reviews"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/designers/" + customerId + "/reviews"))
                .andExpect(status().isNotFound());
        addReview(5, "Should stay hidden", "2026-10-20T08:00:00Z");
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", designerId);
        mvc.perform(get(url())).andExpect(status().isNotFound());
    }

    @Test
    void invalidPaginationReturnsBadRequest() throws Exception {
        mvc.perform(get(url() + "?page=-1")).andExpect(status().isBadRequest());
        mvc.perform(get(url() + "?page=10001")).andExpect(status().isBadRequest());
        mvc.perform(get(url() + "?page=invalid")).andExpect(status().isBadRequest());
    }
}
