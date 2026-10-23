package csit314.renovation.dispute.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class DesignerReportIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordEncoder passwords;

    private Long designerId;
    private Long customerId;
    private final String reportBody = """
            {"reason":"MISLEADING_INFORMATION",
             "description":"The public portfolio appears to contain information that needs review."}
            """;

    @BeforeEach
    void prepare() {
        cleanup();
        String hash = passwords.encode("StrongPassword123!");
        customerId = accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer One",
                "customer1@example.com", hash)).getId();
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer Two",
                "customer2@example.com", hash));
        designerId = accounts.saveAndFlush(UserAccount.newInteriorDesigner(null, "Designer One",
                "designer@example.com", hash)).getId();
        accounts.saveAndFlush(UserAccount.newUserAdmin("Admin", "admin@example.com", hash));
        accounts.saveAndFlush(UserAccount.newPlatformManager("Manager", "manager@example.com", hash));
    }

    @AfterEach
    void cleanup() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        accounts.deleteAll();
    }

    private record Session(MockHttpSession session, String csrfHeader, String token) {}

    private Session start() throws Exception {
        MvcResult csrf = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(csrf.getResponse().getContentAsString());
        return new Session((MockHttpSession) csrf.getRequest().getSession(false),
                token.get("headerName").asText(), token.get("token").asText());
    }

    private Session tokenFor(MockHttpSession session) throws Exception {
        MvcResult csrf = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(csrf.getResponse().getContentAsString());
        return new Session((MockHttpSession) csrf.getRequest().getSession(false),
                token.get("headerName").asText(), token.get("token").asText());
    }

    private Session login(String email) throws Exception {
        Session visitor = start();
        MvcResult result = mvc.perform(post("/api/login").session(visitor.session())
                .header(visitor.csrfHeader(), visitor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email,
                        "password", "StrongPassword123!"))))
                .andExpect(status().isOk()).andReturn();
        return tokenFor((MockHttpSession) result.getRequest().getSession(false));
    }

    private String url() { return "/api/designers/" + designerId + "/reports"; }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            submission(Session actor, String body) {
        var request = post(url()).contentType(MediaType.APPLICATION_JSON).content(body);
        if (actor != null) request.session(actor.session());
        if (actor != null && actor.csrfHeader() != null) {
            request.header(actor.csrfHeader(), actor.token());
        }
        return request;
    }

    @Test
    void customerSubmitsPrivateReportWithReferenceAndCannotSubmitDuplicateOpenReport() throws Exception {
        Session customer = login("customer1@example.com");
        mvc.perform(submission(customer, reportBody)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.reason").value("MISLEADING_INFORMATION"));
        mvc.perform(submission(customer, reportBody)).andExpect(status().isConflict());
        Session secondCustomer = login("customer2@example.com");
        mvc.perform(submission(secondCustomer, reportBody)).andExpect(status().isCreated());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM designer_reports", Integer.class));
    }

    @Test
    void reportSubmissionEnforcesRolesCsrfAndValidation() throws Exception {
        Session visitor = start();
        mvc.perform(submission(visitor, reportBody)).andExpect(status().isUnauthorized());
        mvc.perform(submission(login("designer@example.com"), reportBody))
                .andExpect(status().isForbidden());
        mvc.perform(submission(login("admin@example.com"), reportBody))
                .andExpect(status().isForbidden());
        Session customer = login("customer1@example.com");
        mvc.perform(post(url()).session(customer.session())
                .contentType(MediaType.APPLICATION_JSON).content(reportBody))
                .andExpect(status().isForbidden());
        for (String body : new String[]{"{}", "{\"reason\":\"OTHER\",\"description\":\"short\"}",
                "{\"reason\":\"INVALID\",\"description\":\"A sufficiently lengthy description\"}"}) {
            mvc.perform(submission(customer, body)).andExpect(status().isBadRequest());
        }
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", designerId);
        mvc.perform(submission(customer, reportBody)).andExpect(status().isNotFound());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM designer_reports", Integer.class));
    }

    @Test
    void adminOnlyQueueAndDecisionCreateAuditWithoutModifyingDesigner() throws Exception {
        Session customer = login("customer1@example.com");
        String submissionJson = mvc.perform(submission(customer, reportBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long id = json.readTree(submissionJson).get("id").asLong();
        String list = "/api/admin/designer-reports";
        mvc.perform(get(list)).andExpect(status().isUnauthorized());
        mvc.perform(get(list).session(customer.session())).andExpect(status().isForbidden());
        Session admin = login("admin@example.com");
        mvc.perform(get(list).session(admin.session()).param("status", "OPEN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.openCount").value(1))
                .andExpect(jsonPath("$.reports[0].id").value(id.intValue()));
        mvc.perform(get(list).session(admin.session()).param("q", "REF-" + id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        jdbc.update("INSERT INTO designer_companies(designer_id, company_name) VALUES (?, ?)",
                designerId, "Northwood Studio");
        mvc.perform(get(list).session(admin.session()).param("q", "northwood"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.reports[0].designerName").value("Northwood Studio"));
        mvc.perform(get(list).session(admin.session()).param("status", "INVALID"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(list + "/" + id).session(admin.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.customerId").value(customerId.intValue()));
        String decision = "{\"outcome\":\"RESOLVED\",\"note\":\"Reviewed the submitted information.\"}";
        mvc.perform(put(list + "/" + id + "/decision").session(admin.session())
                .contentType(MediaType.APPLICATION_JSON).content(decision))
                .andExpect(status().isForbidden());
        mvc.perform(put(list + "/" + id + "/decision").session(admin.session())
                .header(admin.csrfHeader(), admin.token())
                .contentType(MediaType.APPLICATION_JSON).content(decision))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.decidedById").isNumber())
                .andExpect(jsonPath("$.decidedAt").exists());
        mvc.perform(put(list + "/" + id + "/decision").session(admin.session())
                .header(admin.csrfHeader(), admin.token())
                .contentType(MediaType.APPLICATION_JSON).content(decision))
                .andExpect(status().isConflict());
        mvc.perform(get(list).session(admin.session()).param("status", "RESOLVED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM designer_reports WHERE status = 'RESOLVED'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_accounts WHERE id = ? AND active = true",
                Integer.class, designerId));
        // The same customer may file a new report after the previous report was closed.
        mvc.perform(submission(customer, reportBody)).andExpect(status().isCreated());
    }

    @Test
    void invalidDecisionsAndNonAdminsCannotSeePrivateDetails() throws Exception {
        Session customer = login("customer1@example.com");
        Long id = json.readTree(mvc.perform(submission(customer, reportBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("id").asLong();
        String url = "/api/admin/designer-reports/" + id;
        mvc.perform(get(url).session(customer.session())).andExpect(status().isForbidden());
        mvc.perform(put(url + "/decision").session(customer.session())
                .header(customer.csrfHeader(), customer.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outcome\":\"DISMISSED\",\"note\":\"The details do not establish an issue.\"}"))
                .andExpect(status().isForbidden());
        Session admin = login("admin@example.com");
        for (String body : new String[]{"{}", "{\"outcome\":\"OPEN\",\"note\":\"Some details\"}",
                "{\"outcome\":\"DISMISSED\",\"note\":\"short\"}"}) {
            mvc.perform(put(url + "/decision").session(admin.session())
                    .header(admin.csrfHeader(), admin.token())
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get(url).session(admin.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPEN"));
        mvc.perform(put(url + "/decision").session(admin.session())
                .header(admin.csrfHeader(), admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outcome\":\"DISMISSED\",\"note\":\"Report was reviewed and no action is required.\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISMISSED"));
    }
}
