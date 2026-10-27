package csit314.renovation.qa.boundary;

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
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectStatus;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
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
class ProjectQuestionIntegrationTest {
    private static final String PASSWORD = "ProjectQATest123!";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired InteriorDesignProjectRepository projects;
    @Autowired PasswordEncoder encoder;

    private Long projectId;
    private Long secondProjectId;
    private Long customerId;
    private Long designerId;

    @BeforeEach
    void setup() {
        cleanup();
        UserAccount designer = UserAccount.newInteriorDesigner(null, "Northline Designer",
                "designer@example.com", encoder.encode(PASSWORD));
        designer.replacePasswordHash(encoder.encode(PASSWORD));
        designerId = accounts.saveAndFlush(designer).getId();
        UserAccount strangerDesigner = UserAccount.newInteriorDesigner(null, "Other Designer",
                "designer2@example.com", encoder.encode(PASSWORD));
        strangerDesigner.replacePasswordHash(encoder.encode(PASSWORD));
        strangerDesigner = accounts.saveAndFlush(strangerDesigner);
        customerId = accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer One",
                "customer@example.com", encoder.encode(PASSWORD))).getId();
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer Two",
                "customer2@example.com", encoder.encode(PASSWORD)));
        projectId = projects.saveAndFlush(new InteriorDesignProject(designer, null, "Warm Minimalist Living",
                "Renovation project details", ProjectStatus.COMPLETED)).getId();
        secondProjectId = projects.saveAndFlush(new InteriorDesignProject(strangerDesigner, null, "Other Studio Project",
                "Other renovation details", ProjectStatus.COMPLETED)).getId();
    }

    @AfterEach
    void cleanup() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        jdbc.update("DELETE FROM user_accounts");
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

    private String questionUrl(Long project) { return "/api/projects/" + project + "/questions"; }
    private String answerUrl(long question) { return "/api/designer/project-questions/" + question + "/answer"; }

    private JsonNode ask(MockHttpSession session, Long project, String content) throws Exception {
        JsonNode token = csrf(session);
        MvcResult result = mvc.perform(post(questionUrl(project)).session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("question", content))))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode answer(MockHttpSession session, long id, String content) throws Exception {
        JsonNode token = csrf(session);
        MvcResult result = mvc.perform(post(answerUrl(id)).session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("answer", content))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void questionIsPrivateUntilAnsweredThenPublicWithoutCustomerIdentity() throws Exception {
        MockHttpSession customer = login("customer@example.com");
        MockHttpSession designer = login("designer@example.com");
        long id = ask(customer, projectId, "  What wood was used in this kitchen? ").get("id").asLong();
        mvc.perform(get(questionUrl(projectId))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/customer/projects/" + projectId + "/questions").session(customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.items[0].question").value("What wood was used in this kitchen?"));
        mvc.perform(get("/api/customer/projects/" + projectId + "/questions")
                .session(login("customer2@example.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/api/designer/project-questions").session(designer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pendingCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id));
        answer(designer, id, "We used solid oak wood.");
        MvcResult result = mvc.perform(get(questionUrl(projectId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].answer").value("We used solid oak wood."))
                .andReturn();
        String publicJson = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(publicJson.contains("customerId"));
        org.junit.jupiter.api.Assertions.assertFalse(publicJson.contains("customer@example.com"));
        mvc.perform(get("/api/customer/projects/" + projectId + "/questions").session(customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/api/designer/project-questions").session(designer).param("status", "answered"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.answeredCount").value(1));
    }

    @Test
    void roleOwnershipCsrfAndDuplicateAnswersAreEnforced() throws Exception {
        MockHttpSession customer = login("customer@example.com");
        MockHttpSession designer = login("designer@example.com");
        MockHttpSession otherDesigner = login("designer2@example.com");
        long id = ask(customer, projectId, "Can you describe the countertop?").get("id").asLong();
        mvc.perform(post(questionUrl(projectId)).session(customer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"Missing CSRF token\"}"))
                .andExpect(status().isForbidden());
        JsonNode designerToken = csrf(designer);
        mvc.perform(post(questionUrl(projectId)).session(designer)
                .header(designerToken.get("headerName").asText(), designerToken.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Is this permitted?\"}"))
                .andExpect(status().isForbidden());
        JsonNode customerToken = csrf(customer);
        mvc.perform(post(answerUrl(id)).session(customer)
                .header(customerToken.get("headerName").asText(), customerToken.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"No\"}"))
                .andExpect(status().isForbidden());
        JsonNode otherToken = csrf(otherDesigner);
        mvc.perform(post(answerUrl(id)).session(otherDesigner)
                .header(otherToken.get("headerName").asText(), otherToken.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"No\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/designer/project-questions").session(otherDesigner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.allCount").value(0));
        mvc.perform(get("/api/designer/project-questions")).andExpect(status().isUnauthorized());
        answer(designer, id, "The counter is quartz.");
        mvc.perform(post(answerUrl(id)).session(designer)
                .header(designerToken.get("headerName").asText(), designerToken.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"Changed\"}"))
                .andExpect(status().isConflict());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM project_questions", Integer.class));
    }

    @Test
    void validatesInputsAndSearchesAnswersWithServerPagination() throws Exception {
        MockHttpSession customer = login("customer@example.com");
        MockHttpSession designer = login("designer@example.com");
        JsonNode token = csrf(customer);
        for (String question : new String[]{"   ", "Short", "x".repeat(601)}) {
            mvc.perform(post(questionUrl(projectId)).session(customer)
                    .header(token.get("headerName").asText(), token.get("token").asText())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("question", question))))
                    .andExpect(status().isBadRequest());
        }
        for (int i = 0; i < 8; i++) {
            long id = ask(customer, projectId, "Which material was selected for room " + i + "?").get("id").asLong();
            answer(designer, id, i % 2 == 0 ? "It was quartz." : "It was timber.");
        }
        mvc.perform(get(questionUrl(projectId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.total").value(8));
        mvc.perform(get(questionUrl(projectId)).param("size", "6").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2));
        mvc.perform(get(questionUrl(projectId)).param("size", "6").param("query", "quartz"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(4));
        mvc.perform(get(questionUrl(projectId)).param("size", "50"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/designer/project-questions").session(designer).param("status", "answered"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.allCount").value(8));
        mvc.perform(get("/api/designer/project-questions").session(designer).param("status", "all")
                .param("projectId", String.valueOf(secondProjectId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void suspendedDesignerProjectHiddenAndAccountDeletionCascades() throws Exception {
        MockHttpSession customer = login("customer@example.com");
        ask(customer, projectId, "Is the project ready for handover?");
        jdbc.update("UPDATE user_accounts SET active = FALSE WHERE id = ?", designerId);
        mvc.perform(get(questionUrl(projectId))).andExpect(status().isNotFound());
        jdbc.update("DELETE FROM user_accounts WHERE id = ?", customerId);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM project_questions", Integer.class));
    }
}
