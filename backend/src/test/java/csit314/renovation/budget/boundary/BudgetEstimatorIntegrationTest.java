package csit314.renovation.budget.boundary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import java.util.LinkedHashMap;
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
class BudgetEstimatorIntegrationTest {
    private static final String PASSWORD = "BudgetExample123!";
    private static final String URL = "/api/customer/budget/estimate";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void setup() {
        cleanup();
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Budget Customer", "budget.customer@example.com",
                encoder.encode(PASSWORD)));
        UserAccount designer = UserAccount.newInteriorDesigner(null, "Budget Designer",
                "budget.designer@example.com", encoder.encode(PASSWORD));
        designer.replacePasswordHash(encoder.encode(PASSWORD));
        accounts.saveAndFlush(designer);
    }

    @AfterEach
    void cleanup() {
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        jdbc.update("DELETE FROM user_accounts");
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult csrf = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(csrf.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) csrf.getRequest().getSession(false);
        MvcResult login = mvc.perform(post("/api/login").session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }

    private JsonNode csrf(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private Map<String, Object> input() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("propertyType", "hdb");
        values.put("propertyCondition", "resale");
        values.put("floorAreaSqm", 95);
        values.put("roomIntensities", Map.of("living", "standard", "kitchen", "extensive",
                "bedrooms", "light", "bathrooms", "standard"));
        values.put("workTypes", java.util.List.of("painting", "flooring", "carpentry", "electrical"));
        values.put("contingencyPercent", 10);
        return values;
    }

    @Test
    void customerReceivesV3EstimateButNoBudgetIsPersisted() throws Exception {
        MockHttpSession customer = login("budget.customer@example.com");
        JsonNode token = csrf(customer);
        mvc.perform(post(URL).session(customer)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.midpoint").value(48519.17))
                .andExpect(jsonPath("$.rooms.length()").value(4))
                .andExpect(jsonPath("$.workRates.length()").value(4))
                .andExpect(jsonPath("$.contingencyPercent").value(10));
    }

    @Test
    void unauthenticatedAndOtherRolesAreDeniedAndCsrfIsRequired() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input())))
                .andExpect(status().isForbidden());
        MockHttpSession customer = login("budget.customer@example.com");
        mvc.perform(post(URL).session(customer).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input())))
                .andExpect(status().isForbidden());
        MockHttpSession designer = login("budget.designer@example.com");
        JsonNode token = csrf(designer);
        mvc.perform(post(URL).session(designer)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input())))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidCustomerInputReturnsClearBadRequest() throws Exception {
        MockHttpSession customer = login("budget.customer@example.com");
        JsonNode token = csrf(customer);
        var values = input();
        values.put("floorAreaSqm", 9);
        mvc.perform(post(URL).session(customer)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(values)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
