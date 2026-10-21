package csit314.renovation.review.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
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
class DesignerReviewIntegrationTest {
    private static final String PASSWORD = "TestPassword123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordEncoder encoder;

    private Long designerId;

    @BeforeEach
    void prepare() {
        cleanup();
        designerId = accounts.saveAndFlush(UserAccount.newInteriorDesigner(null, "Designer",
                "designer@example.com", encoder.encode(PASSWORD))).getId();
    }

    @AfterEach
    void cleanup() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        jdbc.update("DELETE FROM designer_reviews");
        // All other feature tables use cascading references to user_accounts.
        jdbc.update("DELETE FROM user_accounts");
    }

    private UserAccount customer(String email) {
        return accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer", email,
                encoder.encode(PASSWORD)));
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult initial = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(initial.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) initial.getRequest().getSession(false);
        MvcResult result = mvc.perform(post("/api/login").session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private JsonNode csrf(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private String url(Long id) { return "/api/designers/" + id + "/reviews"; }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder review(
            MockHttpSession session, JsonNode token, Long id, String body) {
        var request = post(url(id)).contentType(MediaType.APPLICATION_JSON).content(body);
        if (session != null) request.session(session);
        if (token != null) request.header(token.get("headerName").asText(), token.get("token").asText());
        return request;
    }

    @Test
    void signedInCustomerCanSubmitRatingWithoutCommentAndMultipleReviews() throws Exception {
        customer("customer@example.com");
        MockHttpSession session = login("customer@example.com");
        JsonNode token = csrf(session);
        mvc.perform(review(session, token, designerId, "{\"rating\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").isEmpty());
        mvc.perform(review(session, token, designerId,
                "{\"rating\":3,\"comment\":\"  Helpful designer  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment").value("Helpful designer"));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM designer_reviews", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM designer_reviews WHERE comment IS NULL", Integer.class));
    }

    @Test
    void allRatingsFromOneToFiveAreAllowedAndWhitespaceCommentBecomesNull() throws Exception {
        customer("customer@example.com");
        MockHttpSession session = login("customer@example.com");
        JsonNode token = csrf(session);
        for (int rating = 1; rating <= 5; rating++) {
            mvc.perform(review(session, token, designerId,
                    "{\"rating\":" + rating + ",\"comment\":\"  \"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.comment").isEmpty());
        }
        assertEquals(5, jdbc.queryForObject("SELECT count(*) FROM designer_reviews", Integer.class));
    }

    @Test
    void missingOrOutOfRangeRatingAndOversizedCommentAreRejected() throws Exception {
        customer("customer@example.com");
        MockHttpSession session = login("customer@example.com");
        JsonNode token = csrf(session);
        for (String body : new String[]{"{}", "{\"rating\":0}", "{\"rating\":6}",
                "{\"rating\":null}", "{\"rating\":\"five\"}",
                "{\"rating\":5,\"comment\":\"" + "x".repeat(2001) + "\"}"}) {
            mvc.perform(review(session, token, designerId, body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM designer_reviews", Integer.class));
    }

    @Test
    void visitorsDesignersAndRequestsWithoutCsrfCannotSubmit() throws Exception {
        customer("customer@example.com");
        MvcResult anonymous = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode visitorCsrf = json.readTree(anonymous.getResponse().getContentAsString());
        MockHttpSession visitorSession = (MockHttpSession) anonymous.getRequest().getSession(false);
        mvc.perform(review(visitorSession, visitorCsrf, designerId, "{\"rating\":4}"))
                .andExpect(status().isUnauthorized());

        MockHttpSession designer = login("designer@example.com");
        mvc.perform(review(designer, csrf(designer), designerId, "{\"rating\":4}"))
                .andExpect(status().isForbidden());

        MockHttpSession customer = login("customer@example.com");
        mvc.perform(review(customer, null, designerId, "{\"rating\":4}"))
                .andExpect(status().isForbidden());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM designer_reviews", Integer.class));
    }

    @Test
    void unknownInactiveOrNonDesignerAccountCannotReceiveReview() throws Exception {
        customer("customer@example.com");
        UserAccount anotherCustomer = customer("other@example.com");
        MockHttpSession session = login("customer@example.com");
        JsonNode token = csrf(session);
        mvc.perform(review(session, token, 9999999L, "{\"rating\":4}"))
                .andExpect(status().isNotFound());
        mvc.perform(review(session, token, anotherCustomer.getId(), "{\"rating\":4}"))
                .andExpect(status().isNotFound());
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", designerId);
        mvc.perform(review(session, token, designerId, "{\"rating\":4}"))
                .andExpect(status().isNotFound());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM designer_reviews", Integer.class));
    }

    @Test
    void inactiveCustomerCannotSubmitUsingPreviouslyEstablishedSession() throws Exception {
        UserAccount customer = customer("customer@example.com");
        MockHttpSession session = login("customer@example.com");
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", customer.getId());
        mvc.perform(review(session, csrf(session), designerId, "{\"rating\":5}"))
                .andExpect(status().isForbidden());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM designer_reviews", Integer.class));
    }
}
