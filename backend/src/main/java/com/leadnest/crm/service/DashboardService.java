package com.leadnest.crm.service;

import com.leadnest.crm.domain.DealStage;
import com.leadnest.crm.domain.LeadStatus;
import com.leadnest.crm.domain.Role;
import com.leadnest.crm.domain.UserAccount;
import com.leadnest.crm.dto.ApiDtos.DashboardResponse;
import com.leadnest.crm.dto.ApiDtos.FollowUpResponse;
import com.leadnest.crm.dto.ApiDtos.StatusCount;
import com.leadnest.crm.repository.DealRepository;
import com.leadnest.crm.repository.FollowUpRepository;
import com.leadnest.crm.repository.LeadRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final LeadRepository leads;
    private final DealRepository deals;
    private final FollowUpRepository followUps;
    private final AccessGuard access;

    public DashboardService(
            LeadRepository leads,
            DealRepository deals,
            FollowUpRepository followUps,
            AccessGuard access) {
        this.leads = leads;
        this.deals = deals;
        this.followUps = followUps;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public DashboardResponse summary() {
        UserAccount actor = access.actor();
        boolean admin = actor.getRole() == Role.ADMIN;
        Long agentId = actor.getId();
        Instant now = Instant.now();
        Map<LeadStatus, Long> counts = new EnumMap<>(LeadStatus.class);
        for (LeadStatus status : LeadStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : leads.countByStatus(admin, agentId)) {
            counts.put((LeadStatus) row[0], (Long) row[1]);
        }
        List<StatusCount> byStatus = new ArrayList<>();
        for (LeadStatus status : LeadStatus.values()) {
            byStatus.add(new StatusCount(status, counts.get(status)));
        }
        List<FollowUpResponse> upcoming = followUps.findUpcoming(now, admin, agentId, PageRequest.of(0, 8))
                .stream()
                .map(FollowUpResponse::from)
                .toList();
        return new DashboardResponse(
                leads.countActive(admin, agentId),
                leads.countQualified(admin, agentId),
                money(deals.sumAmount(List.of(DealStage.PROPOSAL, DealStage.NEGOTIATION), admin, agentId)),
                money(deals.sumAmount(List.of(DealStage.WON), admin, agentId)),
                followUps.countOverdue(now, admin, agentId),
                byStatus,
                upcoming);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
