package csit314.renovation.discovery.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csit314.renovation.TestDatabaseGuard;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.category.entity.ProjectCategory;
import csit314.renovation.category.repository.ProjectCategoryRepository;
import csit314.renovation.discovery.control.DiscoveryService;
import csit314.renovation.discovery.control.DiscoveryService.MetadataInput;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectStatus;
import csit314.renovation.portfolio.repository.InteriorDesignProjectRepository;
import java.math.BigDecimal;
import java.util.List;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/** V19 PostgreSQL, HTTP contract and role-isolation tests. Requires a disposable test database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiscoveryIntegrationTest {
    private static final String PASSWORD = "DiscoveryTest123!";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired ProjectCategoryRepository categories;
    @Autowired InteriorDesignProjectRepository projects;
    @Autowired PasswordEncoder encoder;
    @Autowired DiscoveryService discovery;

    private UserAccount designer;
    private UserAccount otherDesigner;

    @BeforeEach
    void prepare() {
        cleanup();
        accounts.saveAndFlush(UserAccount.newPlatformManager("Manager", "manager@example.com",
                encoder.encode(PASSWORD)));
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer A", "customer@example.com",
                encoder.encode(PASSWORD)));
        accounts.saveAndFlush(UserAccount.newCustomer(null, "Customer B", "customer2@example.com",
                encoder.encode(PASSWORD)));
        designer = UserAccount.newInteriorDesigner(null, "Designer A", "designer@example.com",
                encoder.encode(PASSWORD));
        designer.replacePasswordHash(encoder.encode(PASSWORD));
        designer = accounts.saveAndFlush(designer);
        otherDesigner = UserAccount.newInteriorDesigner(null, "Designer B", "designer2@example.com",
                encoder.encode(PASSWORD));
        otherDesigner.replacePasswordHash(encoder.encode(PASSWORD));
        otherDesigner = accounts.saveAndFlush(otherDesigner);
    }

    @AfterEach
    void cleanup() {
        // Tests intentionally delete records. Never run without the existing DB name/opt-in guard.
        TestDatabaseGuard.requireSafeForDestructiveTests(jdbc);
        jdbc.update("DELETE FROM user_accounts");
        jdbc.update("DELETE FROM project_categories");
        jdbc.update("DELETE FROM design_styles");
        jdbc.update("DELETE FROM property_types");
        jdbc.update("DELETE FROM size_ranges");
    }

    private record SessionCsrf(MockHttpSession session, String header, String token) { }

    private SessionCsrf login(String email) throws Exception {
        MvcResult first = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode csrf = json.readTree(first.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) first.getRequest().getSession(false);
        MvcResult response = mvc.perform(post("/api/login").session(session)
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        session = (MockHttpSession) response.getRequest().getSession(false);
        MvcResult refreshed = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        csrf = json.readTree(refreshed.getResponse().getContentAsString());
        return new SessionCsrf(session, csrf.get("headerName").asText(), csrf.get("token").asText());
    }

    private JsonNode createCatalogue(SessionCsrf manager, String kind, Map<String, ?> input)
            throws Exception {
        MvcResult result = mvc.perform(post("/api/discovery/catalogues/" + kind)
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input)))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode savePreferences(SessionCsrf customer, Map<String, ?> input) throws Exception {
        MvcResult result = mvc.perform(put("/api/discovery/customer/preferences")
                .session(customer.session()).header(customer.header(), customer.token())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input)))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private Map<String, Object> preferencePayload(long category, long style, long property, long size) {
        return Map.of("categoryIds", List.of(category), "styleIds", List.of(style),
                "propertyTypeIds", List.of(property), "sizeRangeId", size);
    }

    private MockMultipartHttpServletRequestBuilder projectRequest(String url, String title,
            long category, long property, BigDecimal area, long primaryStyle, boolean editing)
            throws Exception {
        Map<String, Object> metadata = Map.of("propertyTypeId", property,
                "floorAreaSqm", area, "primaryStyleId", primaryStyle,
                "secondaryStyleIds", List.of());
        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("title", title);
        request.put("description", "Renovation portfolio project");
        request.put("categoryId", category);
        request.put("status", "COMPLETED");
        request.put("metadata", metadata);
        if (editing) request.put("keepImageIds", List.of());
        MockMultipartHttpServletRequestBuilder builder = multipart(url)
                .file(new MockMultipartFile("project", "project.json", "application/json",
                        json.writeValueAsBytes(request)));
        if (editing) builder.with(r -> { r.setMethod("PUT"); return r; });
        return builder;
    }

    private long createProjectFixture(UserAccount owner, ProjectCategory category, String title,
            long property, BigDecimal area, long style) {
        long id = projects.saveAndFlush(new InteriorDesignProject(owner, category, title,
                "Renovation portfolio project", ProjectStatus.COMPLETED)).getId();
        discovery.saveMetadata(id, new MetadataInput(property, area, style, List.of()), true);
        return id;
    }

    @Test
    void managerCataloguesEnforceUniqueNamesSizeRangesRolesAndCsrf() throws Exception {
        SessionCsrf manager = login("manager@example.com");
        SessionCsrf customer = login("customer@example.com");
        long style = createCatalogue(manager, "styles", Map.of("name", "Minimalist")).get("id").asLong();
        mvc.perform(get("/api/discovery/catalogues/styles"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Minimalist"));
        mvc.perform(post("/api/discovery/catalogues/styles").session(manager.session())
                .header(manager.header(), manager.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" minimalist \"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/discovery/catalogues/styles").session(customer.session())
                .header(customer.header(), customer.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Industrial\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/discovery/catalogues/styles").session(manager.session())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Industrial\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/discovery/catalogues/styles/" + style)
                .session(manager.session()).header(manager.header(), manager.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Modern\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Modern"));

        createCatalogue(manager, "size-ranges", Map.of("name", "Small", "minSqm", 0, "maxSqm", 80));
        createCatalogue(manager, "size-ranges", Map.of("name", "Medium", "minSqm", 80, "maxSqm", 160));
        mvc.perform(post("/api/discovery/catalogues/size-ranges").session(manager.session())
                .header(manager.header(), manager.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Overlap\",\"minSqm\":79.99,\"maxSqm\":120}"))
                .andExpect(status().isConflict());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM size_ranges", Integer.class));
        mvc.perform(delete("/api/discovery/catalogues/styles/" + style)
                .session(manager.session()).header(manager.header(), manager.token()))
                .andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM design_styles", Integer.class));
    }

    @Test
    void customerPreferencesPersistAreIsolatedAndRejectBadSelections() throws Exception {
        SessionCsrf manager = login("manager@example.com");
        SessionCsrf customer = login("customer@example.com");
        SessionCsrf otherCustomer = login("customer2@example.com");
        SessionCsrf designerSession = login("designer@example.com");
        long cat = createCatalogue(manager, "categories", Map.of("name", "Kitchen")).get("id").asLong();
        long style = createCatalogue(manager, "styles", Map.of("name", "Japandi")).get("id").asLong();
        long property = createCatalogue(manager, "property-types", Map.of("name", "HDB")).get("id").asLong();
        long size = createCatalogue(manager, "size-ranges", Map.of("name", "Small", "minSqm", 0, "maxSqm", 80))
                .get("id").asLong();
        JsonNode saved = savePreferences(customer, preferencePayload(cat, style, property, size));
        assertEquals("COMPLETED", saved.get("onboardingState").asText());
        assertEquals(style, saved.get("styleIds").get(0).asLong());
        mvc.perform(get("/api/discovery/customer/preferences").session(otherCustomer.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.styleIds.length()").value(0));
        mvc.perform(get("/api/discovery/customer/preferences").session(designerSession.session()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/discovery/customer/preferences"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/discovery/customer/preferences").session(customer.session())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/discovery/customer/preferences").session(customer.session())
                .header(customer.header(), customer.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"styleIds\":[999999999]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/discovery/customer/preferences").session(customer.session())
                .header(customer.header(), customer.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"styleIds\":[" + style + "," + style + "]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/discovery/customer/preferences/skip")
                .session(otherCustomer.session()).header(otherCustomer.header(), otherCustomer.token()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/discovery/customer/preferences").session(otherCustomer.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.onboardingState").value("SKIPPED"));
        mvc.perform(post("/api/discovery/customer/preferences/skip")
                .session(customer.session()).header(customer.header(), customer.token()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/discovery/customer/preferences").session(customer.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.onboardingState").value("COMPLETED"))
                .andExpect(jsonPath("$.styleIds[0]").value(style));
    }

    @Test
    void designerMetadataCreateEditPrecisionAndPublicFilteringWorkTogether() throws Exception {
        SessionCsrf manager = login("manager@example.com");
        SessionCsrf owner = login("designer@example.com");
        SessionCsrf stranger = login("designer2@example.com");
        long cat = createCatalogue(manager, "categories", Map.of("name", "Residential")).get("id").asLong();
        long style = createCatalogue(manager, "styles", Map.of("name", "Minimalist")).get("id").asLong();
        long otherStyle = createCatalogue(manager, "styles", Map.of("name", "Industrial")).get("id").asLong();
        long property = createCatalogue(manager, "property-types", Map.of("name", "Condominium")).get("id").asLong();
        long small = createCatalogue(manager, "size-ranges", Map.of("name", "Under 80", "minSqm", 0, "maxSqm", 80))
                .get("id").asLong();
        MvcResult result = mvc.perform(projectRequest("/api/designer/projects", "Warm Kitchen", cat,
                        property, new BigDecimal("79.99"), style, false).session(owner.session())
                        .header(owner.header(), owner.token()))
                .andExpect(status().isCreated()).andReturn();
        long projectId = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/discovery/projects/" + projectId + "/metadata"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.propertyTypeId").value(property))
                .andExpect(jsonPath("$.floorAreaSqm").value(79.99))
                .andExpect(jsonPath("$.styles[0].role").value("PRIMARY"));
        mvc.perform(get("/api/projects").param("propertyTypeId", String.valueOf(property))
                .param("sizeRangeId", String.valueOf(small)).param("styleIds", String.valueOf(style)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/projects").param("styleIds", String.valueOf(otherStyle)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/discovery/designer/projects/" + projectId + "/metadata")
                .session(stranger.session())).andExpect(status().isNotFound());

        for (String invalid : List.of("0", "-1", "12.345", "99999999.999", "100000000")) {
            mvc.perform(projectRequest("/api/designer/projects", "Invalid Floor", cat, property,
                            new BigDecimal(invalid), style, false).session(owner.session())
                            .header(owner.header(), owner.token()))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(1, projects.count());
        mvc.perform(projectRequest("/api/designer/projects/" + projectId, "Bad Edit", cat,
                        property, new BigDecimal("80.001"), otherStyle, true)
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/discovery/projects/" + projectId + "/metadata"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.floorAreaSqm").value(79.99));
        mvc.perform(projectRequest("/api/designer/projects/" + projectId, "Updated Kitchen", cat,
                        property, new BigDecimal("80.00"), otherStyle, true)
                        .session(stranger.session()).header(stranger.header(), stranger.token()))
                .andExpect(status().isNotFound());
        mvc.perform(projectRequest("/api/designer/projects/" + projectId, "Updated Kitchen", cat,
                        property, new BigDecimal("80.00"), otherStyle, true)
                        .session(owner.session()).header(owner.header(), owner.token()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/discovery/projects/" + projectId + "/metadata"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.floorAreaSqm").value(80.0))
                .andExpect(jsonPath("$.styles[0].name").value("Industrial"));
        mvc.perform(get("/api/projects").param("sizeRangeId", String.valueOf(small)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM project_metadata", Integer.class));
    }

    @Test
    void recommendationsRankByPreferencesExcludeSavedAndHideInactiveDesigner() throws Exception {
        SessionCsrf manager = login("manager@example.com");
        SessionCsrf customer = login("customer@example.com");
        long cat = createCatalogue(manager, "categories", Map.of("name", "Kitchen")).get("id").asLong();
        long matchedStyle = createCatalogue(manager, "styles", Map.of("name", "Japandi")).get("id").asLong();
        long otherStyle = createCatalogue(manager, "styles", Map.of("name", "Industrial")).get("id").asLong();
        long property = createCatalogue(manager, "property-types", Map.of("name", "HDB")).get("id").asLong();
        long size = createCatalogue(manager, "size-ranges", Map.of("name", "Under 80", "minSqm", 0, "maxSqm", 80))
                .get("id").asLong();
        ProjectCategory category = categories.findById(cat).orElseThrow();
        long match = createProjectFixture(designer, category, "Japandi Kitchen", property,
                new BigDecimal("65.50"), matchedStyle);
        long mismatch = createProjectFixture(otherDesigner, category, "Industrial Kitchen", property,
                new BigDecimal("65.50"), otherStyle);
        savePreferences(customer, preferencePayload(cat, matchedStyle, property, size));

        MvcResult response = mvc.perform(get("/api/discovery/projects").session(customer.session())
                .param("reel", "false")).andExpect(status().isOk())
                .andExpect(jsonPath("$.personalised").value(true))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andReturn();
        JsonNode items = json.readTree(response.getResponse().getContentAsString()).get("items");
        assertEquals(match, items.get(0).get("id").asLong());
        assertTrue(items.get(0).get("score").asInt() > items.get(1).get("score").asInt());
        mvc.perform(get("/api/discovery/designers").session(customer.session()).param("limit", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(designer.getId()))
                .andExpect(jsonPath("$[1].id").value(otherDesigner.getId()));
        mvc.perform(get("/api/discovery/projects").param("reel", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.personalised").value(false))
                .andExpect(jsonPath("$.items[0].score").value(org.hamcrest.Matchers.nullValue()));

        mvc.perform(put("/api/customer/favourites/" + match).session(customer.session())
                .header(customer.header(), customer.token())).andExpect(status().isNoContent());
        mvc.perform(get("/api/discovery/projects").session(customer.session())
                .param("reel", "false")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(mismatch));
        jdbc.update("UPDATE user_accounts SET active = false WHERE id = ?", otherDesigner.getId());
        mvc.perform(get("/api/discovery/projects").param("reel", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(match));
        mvc.perform(get("/api/discovery/projects/" + mismatch + "/metadata"))
                .andExpect(status().isNotFound());
    }
}
