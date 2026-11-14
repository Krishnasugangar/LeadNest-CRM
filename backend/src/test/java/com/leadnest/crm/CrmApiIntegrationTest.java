package com.leadnest.crm;

import com.leadnest.crm.dto.ApiDtos.DealWriteRequest;
import com.leadnest.crm.service.DealService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CrmApiIntegrationTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @org.springframework.test.context.DynamicPropertySource
    static void databaseProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("leadnest.admin.email", () -> "admin@leadnest.test");
        registry.add("leadnest.admin.password", () -> "AdminPass123");
        registry.add("leadnest.admin.name", () -> "Ada Admin");
        registry.add("leadnest.demo-data", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DealService dealService;

    @Test
    void coreWorkflowPersistsThroughQualificationDealActivityAndClose() throws Exception {
        MockHttpSession admin = login("admin@leadnest.test", "AdminPass123");
        MvcResult agentResult = mockMvc.perform(post("/api/agents")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mina Agent","email":"mina.workflow@leadnest.test","password":"AgentPass123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AGENT"))
                .andReturn();
        long agentId = idOf(agentResult.getResponse().getContentAsString());

        MockHttpSession agent = login("mina.workflow@leadnest.test", "AgentPass123");
        MvcResult leadResult = mockMvc.perform(post("/api/leads")
                        .session(agent)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Riya Shah","email":"riya.workflow@example.com","phone":"9845098450","company":"Shah Studio","source":"REFERRAL","notes":"Met at a market"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.assignedAgentId").value(agentId))
                .andReturn();
        long leadId = idOf(leadResult.getResponse().getContentAsString());

        mockMvc.perform(post("/api/leads/" + leadId + "/status")
                        .session(agent)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONTACTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTACTED"));
        mockMvc.perform(post("/api/leads/" + leadId + "/status")
                        .session(agent)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"QUALIFIED\"}"))
                .andExpect(status().isOk());

        MvcResult dealResult = mockMvc.perform(post("/api/leads/" + leadId + "/deal")
                        .session(agent)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Annual retainer","amount":250000.50,"expectedCloseDate":"2026-12-15"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("PROPOSAL"))
                .andExpect(jsonPath("$.leadId").value(leadId))
                .andReturn();
        long dealId = idOf(dealResult.getResponse().getContentAsString());

        mockMvc.perform(post("/api/leads/" + leadId + "/activities")
                        .session(agent)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CALL\",\"description\":\"Discussed scope and timeline\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorName").value("Mina Agent"));

        String due = Instant.now().minusSeconds(3600).toString();
        MvcResult followUp = mockMvc.perform(post("/api/leads/" + leadId + "/follow-ups")
                        .session(agent)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Send the revised quote\",\"dueAt\":\"" + due + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("OVERDUE"))
                .andReturn();
        long followUpId = idOf(followUp.getResponse().getContentAsString());
        mockMvc.perform(post("/api/follow-ups/" + followUpId + "/complete")
                        .session(agent)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(post("/api/deals/" + dealId + "/stage")
                        .session(agent)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"NEGOTIATION\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/deals/" + dealId + "/stage")
                        .session(agent)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"WON\",\"closingReason\":\"Signed the order\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("WON"));

        mockMvc.perform(get("/api/leads/" + leadId).session(agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lead.status").value("QUALIFIED"))
                .andExpect(jsonPath("$.deal.stage").value("WON"))
                .andExpect(jsonPath("$.activities[0].description").value("Discussed scope and timeline"))
                .andExpect(jsonPath("$.history[0].eventType").value("CREATED"));

        Integer deals = jdbcTemplate.queryForObject("select count(*) from deals where lead_id = ?", Integer.class, leadId);
        Integer activities = jdbcTemplate.queryForObject("select count(*) from activities where lead_id = ?", Integer.class, leadId);
        assertThat(deals).isEqualTo(1);
        assertThat(activities).isEqualTo(1);
    }

    @Test
    void agentCannotReadOrMutateAnotherAgentsRecords() throws Exception {
        MockHttpSession admin = login("admin@leadnest.test", "AdminPass123");
        mockMvc.perform(post("/api/agents")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Owner Agent","email":"owner.auth@leadnest.test","password":"AgentPass123"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/agents")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Other Agent","email":"other.auth@leadnest.test","password":"AgentPass123"}
                                """))
                .andExpect(status().isOk());

        MockHttpSession owner = login("owner.auth@leadnest.test", "AgentPass123");
        MockHttpSession other = login("other.auth@leadnest.test", "AgentPass123");
        MvcResult lead = mockMvc.perform(post("/api/leads")
                        .session(owner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Private Lead","phone":"9988776655","source":"PHONE"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long leadId = idOf(lead.getResponse().getContentAsString());

        mockMvc.perform(get("/api/leads/" + leadId).session(other))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/leads/" + leadId + "/status")
                        .session(other)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONTACTED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/leads").session(other).param("agentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.name == 'Private Lead')]").isEmpty());
        mockMvc.perform(get("/api/agents").session(other))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/leads/" + leadId + "/assign")
                        .session(other)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":1}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/leads/" + leadId).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lead.name").value("Private Lead"));
    }

    @Test
    void deactivatingAnAgentEndsTheExistingSession() throws Exception {
        MockHttpSession admin = login("admin@leadnest.test", "AdminPass123");
        MvcResult created = mockMvc.perform(post("/api/agents")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Blocked Agent","email":"blocked.session@leadnest.test","password":"AgentPass123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long agentId = idOf(created.getResponse().getContentAsString());
        MockHttpSession agent = login("blocked.session@leadnest.test", "AgentPass123");
        mockMvc.perform(get("/api/dashboard").session(agent)).andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/agents/" + agentId + "/active")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/leads").session(agent))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"blocked.session@leadnest.test","password":"AgentPass123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidTransitionsLostWithoutReasonAndBadInput() throws Exception {
        MockHttpSession admin = login("admin@leadnest.test", "AdminPass123");
        MvcResult lead = mockMvc.perform(post("/api/leads")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Transition Lead","email":"transition@example.com","source":"WEBSITE"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long leadId = idOf(lead.getResponse().getContentAsString());

        mockMvc.perform(post("/api/leads/" + leadId + "/status")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"QUALIFIED\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/leads/" + leadId + "/status")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOST\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/leads")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"No Contact","source":"OTHER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        mockMvc.perform(post("/api/leads")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bad Email","email":"nope","source":"OTHER"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateConversionIsRejectedAndTheLeadConstraintHolds() throws Exception {
        MockHttpSession admin = login("admin@leadnest.test", "AdminPass123");
        MvcResult lead = mockMvc.perform(post("/api/leads")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Convert Twice","phone":"9000000001","source":"SOCIAL_MEDIA"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long leadId = idOf(lead.getResponse().getContentAsString());
        mockMvc.perform(post("/api/leads/" + leadId + "/status").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONTACTED\"}")).andExpect(status().isOk());
        mockMvc.perform(post("/api/leads/" + leadId + "/status").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"QUALIFIED\"}")).andExpect(status().isOk());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Integer> codes = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < 2; i++) {
            int attempt = i;
            pool.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        "admin@leadnest.test",
                        "AdminPass123",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
                try {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    dealService.convert(leadId, new DealWriteRequest("Attempt " + attempt, new BigDecimal("1500.00"), LocalDate.of(2026, 11, 1)));
                    codes.add(200);
                } catch (com.leadnest.crm.exception.ApiException exception) {
                    codes.add(exception.getStatus().value());
                } catch (Exception exception) {
                    codes.add(500);
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        assertThat(codes).containsExactlyInAnyOrder(200, 409);
        Integer count = jdbcTemplate.queryForObject("select count(*) from deals where lead_id = ?", Integer.class, leadId);
        assertThat(count).isEqualTo(1);

        Integer inserted = null;
        try {
            inserted = jdbcTemplate.update("""
                    insert into deals (title, lead_id, amount, stage, expected_close_date, created_at, updated_at)
                    values ('Second', ?, 10.00, 'PROPOSAL', '2026-11-02', utc_timestamp(6), utc_timestamp(6))
                    """, leadId);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            inserted = -1;
        }
        assertThat(inserted).isEqualTo(-1);

        mockMvc.perform(post("/api/leads/" + leadId + "/deal")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Another","amount":10.00,"expectedCloseDate":"2026-11-02"}
                                """))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/leads/" + leadId + "/status")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOST\",\"lostReason\":\"Too late\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/leads")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Zero Deal Setup","email":"zero.deal@example.com","source":"PHONE"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsNonPositiveDealAmount() throws Exception {
        MockHttpSession admin = login("admin@leadnest.test", "AdminPass123");
        MvcResult lead = mockMvc.perform(post("/api/leads")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Amount Lead","email":"amount.lead@example.com","source":"WEBSITE"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long leadId = idOf(lead.getResponse().getContentAsString());
        mockMvc.perform(post("/api/leads/" + leadId + "/status").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONTACTED\"}")).andExpect(status().isOk());
        mockMvc.perform(post("/api/leads/" + leadId + "/status").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"QUALIFIED\"}")).andExpect(status().isOk());
        mockMvc.perform(post("/api/leads/" + leadId + "/deal")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Free","amount":0,"expectedCloseDate":"2026-11-02"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private long idOf(String json) {
        Matcher matcher = ID_PATTERN.matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException(json);
        }
        return Long.parseLong(matcher.group(1));
    }
}
