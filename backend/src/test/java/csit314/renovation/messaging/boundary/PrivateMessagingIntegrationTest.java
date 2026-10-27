package csit314.renovation.messaging.boundary;

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
class PrivateMessagingIntegrationTest {
    private static final String PASSWORD = "MessagingTest123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordEncoder encoder;

    private Long designerId;
    private Long customerId;
    private Long secondCustomerId;
    private Long secondDesignerId;

    @BeforeEach
    void prepare() {
        cleanup();
        UserAccount designer = UserAccount.newInteriorDesigner(null, "Northline Designer",
                "designer@example.com", encoder.encode(PASSWORD));
        designer.replacePasswordHash(encoder.encode(PASSWORD));
        designerId = accounts.saveAndFlush(designer).getId();
        UserAccount secondDesigner = UserAccount.newInteriorDesigner(null, "Second Designer",
                "designer2@example.com", encoder.encode(PASSWORD));
        secondDesigner.replacePasswordHash(encoder.encode(PASSWORD));
        secondDesignerId = accounts.saveAndFlush(secondDesigner).getId();
        customerId = accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer One",
                "customer@example.com", encoder.encode(PASSWORD))).getId();
        secondCustomerId = accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer Two",
                "customer2@example.com", encoder.encode(PASSWORD))).getId();
    }

    @AfterEach
    void cleanup() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        // V17 ON DELETE CASCADE cleans both private feature tables.
        jdbc.update("DELETE FROM user_accounts");
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult initial = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(initial.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) initial.getRequest().getSession(false);
        MvcResult loggedIn = mvc.perform(post("/api/login").session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) loggedIn.getRequest().getSession(false);
    }

    private JsonNode csrf(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode send(MockHttpSession session, String path, String body) throws Exception {
        JsonNode token = csrf(session);
        MvcResult result = mvc.perform(post(path).session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("body", body))))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private String startUrl(Long id) { return "/api/messages/designers/" + id + "/messages"; }
    private String threadUrl(long id) { return "/api/messages/conversations/" + id + "/messages"; }

    @Test
    void customerStartsConversationDesignerRepliesAndPairIsUnique() throws Exception {
        MockHttpSession customer = login("customer@example.com");
        MockHttpSession designer = login("designer@example.com");
        JsonNode first = send(customer, startUrl(designerId), "  Hi, can we discuss my kitchen?  ");
        long threadId = first.get("conversationId").asLong();
        assertEquals("Hi, can we discuss my kitchen?", first.get("body").asText());
        assertEquals(customerId.longValue(), first.get("senderId").asLong());

        JsonNode response = send(designer, threadUrl(threadId), "Absolutely, what is your timeline?");
        assertEquals(designerId.longValue(), response.get("senderId").asLong());
        assertEquals(threadId, response.get("conversationId").asLong());
        send(customer, startUrl(designerId), "We are hoping for December.");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM private_conversations", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM private_messages", Integer.class));

        mvc.perform(get("/api/messages/conversations").session(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(threadId))
                .andExpect(jsonPath("$[0].counterpartId").value(designerId))
                .andExpect(jsonPath("$[0].lastMessagePreview").value("We are hoping for December."));
        mvc.perform(get("/api/messages/conversations").session(designer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].counterpartId").value(customerId));
        mvc.perform(get(threadUrl(threadId)).session(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(3))
                .andExpect(jsonPath("$.messages[1].body").value("Absolutely, what is your timeline?"))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void outsidersCannotReadOrSendAndCsrfIsRequired() throws Exception {
        MockHttpSession customer = login("customer@example.com");
        MockHttpSession strangerCustomer = login("customer2@example.com");
        MockHttpSession strangerDesigner = login("designer2@example.com");
        accounts.saveAndFlush(UserAccount.newUserAdmin("Admin", "admin@example.com",
                encoder.encode(PASSWORD)));
        MockHttpSession admin = login("admin@example.com");
        long threadId = send(customer, startUrl(designerId), "Private message").get("conversationId").asLong();
        mvc.perform(get(threadUrl(threadId)).session(strangerCustomer)).andExpect(status().isNotFound());
        mvc.perform(get(threadUrl(threadId)).session(strangerDesigner)).andExpect(status().isNotFound());
        JsonNode outsiderToken = csrf(strangerCustomer);
        mvc.perform(post(threadUrl(threadId)).session(strangerCustomer)
                .header(outsiderToken.get("headerName").asText(), outsiderToken.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Hi\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/messages/conversations")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/messages/conversations").session(admin))
                .andExpect(status().isForbidden());
        mvc.perform(post(startUrl(designerId)).session(customer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"No CSRF\"}"))
                .andExpect(status().isForbidden());
        JsonNode designerToken = csrf(strangerDesigner);
        mvc.perform(post(startUrl(secondDesignerId)).session(strangerDesigner)
                .header(designerToken.get("headerName").asText(), designerToken.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Hello\"}"))
                .andExpect(status().isForbidden());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM private_messages", Integer.class));
    }

    @Test
    void emptyTooLongOrInvalidRecipientCannotStartConversation() throws Exception {
        MockHttpSession customer = login("customer@example.com");
        JsonNode token = csrf(customer);
        for (String body : new String[]{"", "  ", "a".repeat(2001)}) {
            mvc.perform(post(startUrl(designerId)).session(customer)
                    .header(token.get("headerName").asText(), token.get("token").asText())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("body", body))))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post(startUrl(customerId)).session(customer)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Hi\"}"))
                .andExpect(status().isNotFound());
        jdbc.update("UPDATE user_accounts SET active = FALSE WHERE id = ?", secondDesignerId);
        mvc.perform(post(startUrl(secondDesignerId)).session(customer)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Hi\"}"))
                .andExpect(status().isNotFound());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM private_conversations", Integer.class));
    }

    @Test
    void messageHistoryIsPaginatedAndDeletingAccountErasesPrivateMessages() throws Exception {
        MockHttpSession customer = login("customer@example.com");
        long threadId = -1;
        for (int i = 0; i < 52; i++) {
            threadId = send(customer, startUrl(designerId), "Message " + i).get("conversationId").asLong();
        }
        MvcResult latest = mvc.perform(get(threadUrl(threadId)).session(customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.messages.length()").value(50)).andReturn();
        JsonNode messages = json.readTree(latest.getResponse().getContentAsString()).get("messages");
        long firstId = messages.get(0).get("id").asLong();
        assertEquals("Message 2", messages.get(0).get("body").asText());
        mvc.perform(get(threadUrl(threadId)).session(customer).param("before", String.valueOf(firstId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.hasMore").value(false));
        jdbc.update("DELETE FROM user_accounts WHERE id = ?", customerId);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM private_conversations", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM private_messages", Integer.class));
    }

    @Test
    void designerWithTemporaryPasswordCannotUseMessagingUntilChanged() throws Exception {
        jdbc.update("UPDATE user_accounts SET password_change_required = TRUE WHERE id = ?", secondDesignerId);
        MockHttpSession pending = login("designer2@example.com");
        mvc.perform(get("/api/messages/conversations").session(pending))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/messages/conversations/99999/messages").session(pending))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspendedRecipientCannotReceiveFurtherRepliesAndSuspendedSenderLosesAccess() throws Exception {
        MockHttpSession customer = login("customer@example.com");
        MockHttpSession designer = login("designer@example.com");
        long threadId = send(customer, startUrl(designerId), "Hello").get("conversationId").asLong();
        jdbc.update("UPDATE user_accounts SET active = FALSE WHERE id = ?", designerId);
        JsonNode token = csrf(customer);
        mvc.perform(post(threadUrl(threadId)).session(customer)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Second\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/messages/conversations").session(designer))
                .andExpect(status().isUnauthorized());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM private_messages", Integer.class));
    }
}
