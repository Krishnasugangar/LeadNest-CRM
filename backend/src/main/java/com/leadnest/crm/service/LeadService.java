package com.leadnest.crm.service;

import com.leadnest.crm.domain.HistoryEventType;
import com.leadnest.crm.domain.Lead;
import com.leadnest.crm.domain.LeadStatus;
import com.leadnest.crm.domain.Role;
import com.leadnest.crm.domain.UserAccount;
import com.leadnest.crm.dto.ApiDtos.ActivityResponse;
import com.leadnest.crm.dto.ApiDtos.AssignLeadRequest;
import com.leadnest.crm.dto.ApiDtos.DealResponse;
import com.leadnest.crm.dto.ApiDtos.FollowUpResponse;
import com.leadnest.crm.dto.ApiDtos.HistoryResponse;
import com.leadnest.crm.dto.ApiDtos.LeadDetailResponse;
import com.leadnest.crm.dto.ApiDtos.LeadResponse;
import com.leadnest.crm.dto.ApiDtos.LeadWriteRequest;
import com.leadnest.crm.dto.ApiDtos.PageResponse;
import com.leadnest.crm.dto.ApiDtos.StatusChangeRequest;
import com.leadnest.crm.exception.ApiException;
import com.leadnest.crm.repository.ActivityRepository;
import com.leadnest.crm.repository.DealRepository;
import com.leadnest.crm.repository.FollowUpRepository;
import com.leadnest.crm.repository.LeadHistoryRepository;
import com.leadnest.crm.repository.LeadRepository;
import com.leadnest.crm.repository.UserAccountRepository;
import com.leadnest.crm.support.PageRequests;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class LeadService {

    private final LeadRepository leads;
    private final UserAccountRepository users;
    private final DealRepository deals;
    private final ActivityRepository activities;
    private final FollowUpRepository followUps;
    private final LeadHistoryRepository historyRepository;
    private final AccessGuard access;
    private final HistoryRecorder history;

    public LeadService(
            LeadRepository leads,
            UserAccountRepository users,
            DealRepository deals,
            ActivityRepository activities,
            FollowUpRepository followUps,
            LeadHistoryRepository historyRepository,
            AccessGuard access,
            HistoryRecorder history) {
        this.leads = leads;
        this.users = users;
        this.deals = deals;
        this.activities = activities;
        this.followUps = followUps;
        this.historyRepository = historyRepository;
        this.access = access;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> search(
            String search,
            LeadStatus status,
            Long agentId,
            boolean includeArchived,
            int page,
            int size) {
        UserAccount actor = access.actor();
        Long scopedAgent = actor.getRole() == Role.ADMIN ? agentId : null;
        Page<LeadResponse> result = leads.findAll(
                        LeadSpecifications.matching(actor, search, status, scopedAgent, includeArchived),
                        PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(LeadResponse::from);
        return PageRequests.toResponse(result);
    }

    @Transactional
    public LeadResponse create(LeadWriteRequest request) {
        UserAccount actor = access.actor();
        ContactRules.Parsed contact = ContactRules.parse(request.email(), request.phone());
        UserAccount assignee = resolveAssignee(actor, request.agentId());
        Lead lead = new Lead();
        applyFields(lead, request, contact);
        lead.setStatus(LeadStatus.NEW);
        lead.setAssignedAgent(assignee);
        lead.setArchived(false);
        Lead saved = leads.save(lead);
        history.record(saved, actor, HistoryEventType.CREATED, "Lead created");
        if (!assignee.getId().equals(actor.getId())) {
            history.record(saved, actor, HistoryEventType.ASSIGNED, "Assigned to " + assignee.getName());
        }
        return LeadResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public LeadDetailResponse get(Long id) {
        Lead lead = access.requireLead(id);
        DealResponse deal = deals.findByLeadId(id).map(DealResponse::from).orElse(null);
        List<ActivityResponse> activityRows = activities.findForLead(id).stream().map(ActivityResponse::from).toList();
        List<FollowUpResponse> followUpRows = followUps.findForLead(id).stream().map(FollowUpResponse::from).toList();
        List<HistoryResponse> historyRows = historyRepository.findForLead(id).stream().map(HistoryResponse::from).toList();
        return new LeadDetailResponse(LeadResponse.from(lead), activityRows, followUpRows, deal, historyRows);
    }

    @Transactional
    public LeadResponse update(Long id, LeadWriteRequest request) {
        UserAccount actor = access.actor();
        Lead lead = access.requireLeadForUpdate(id);
        ensureMutable(lead);
        ContactRules.Parsed contact = ContactRules.parse(request.email(), request.phone());
        applyFields(lead, request, contact);
        if (actor.getRole() == Role.ADMIN && request.agentId() != null
                && !request.agentId().equals(lead.getAssignedAgent().getId())) {
            UserAccount assignee = requireActiveAssignee(request.agentId());
            lead.setAssignedAgent(assignee);
            history.record(lead, actor, HistoryEventType.ASSIGNED, "Assigned to " + assignee.getName());
        }
        return LeadResponse.from(lead);
    }

    @Transactional
    public LeadResponse changeStatus(Long id, StatusChangeRequest request) {
        UserAccount actor = access.actor();
        Lead lead = access.requireLeadForUpdate(id);
        ensureMutable(lead);
        boolean dealExists = deals.existsByLeadId(lead.getId());
        String reason = ContactRules.blankToNull(request.lostReason());
        LeadStatusPolicy.assertCanChange(lead.getStatus(), request.status(), dealExists, reason);
        LeadStatus previous = lead.getStatus();
        lead.setStatus(request.status());
        lead.setLostReason(request.status() == LeadStatus.LOST ? reason : null);
        String detail = "Status changed from " + previous + " to " + request.status();
        if (request.status() == LeadStatus.LOST) {
            detail = detail + ". Reason: " + reason;
        }
        history.record(lead, actor, HistoryEventType.STATUS_CHANGED, detail);
        return LeadResponse.from(lead);
    }

    @Transactional
    public LeadResponse assign(Long id, AssignLeadRequest request) {
        UserAccount actor = access.actor();
        access.requireAdmin();
        Lead lead = access.requireLeadForUpdate(id);
        UserAccount assignee = requireActiveAssignee(request.agentId());
        if (!assignee.getId().equals(lead.getAssignedAgent().getId())) {
            lead.setAssignedAgent(assignee);
            history.record(lead, actor, HistoryEventType.ASSIGNED, "Assigned to " + assignee.getName());
        }
        return LeadResponse.from(lead);
    }

    @Transactional
    public LeadResponse archive(Long id) {
        UserAccount actor = access.actor();
        Lead lead = access.requireLeadForUpdate(id);
        if (!lead.isArchived()) {
            lead.setArchived(true);
            lead.setArchivedAt(Instant.now());
            history.record(lead, actor, HistoryEventType.ARCHIVED, "Lead archived");
        }
        return LeadResponse.from(lead);
    }

    @Transactional
    public LeadResponse restore(Long id) {
        UserAccount actor = access.actor();
        Lead lead = access.requireLeadForUpdate(id);
        if (lead.isArchived()) {
            lead.setArchived(false);
            lead.setArchivedAt(null);
            history.record(lead, actor, HistoryEventType.RESTORED, "Lead restored");
        }
        return LeadResponse.from(lead);
    }

    private void applyFields(Lead lead, LeadWriteRequest request, ContactRules.Parsed contact) {
        lead.setName(request.name().trim());
        lead.setEmail(contact.email());
        lead.setPhone(contact.phone());
        lead.setCompany(ContactRules.blankToNull(request.company()));
        lead.setSource(request.source());
        lead.setNotes(ContactRules.blankToNull(request.notes()));
    }

    private UserAccount resolveAssignee(UserAccount actor, Long agentId) {
        if (actor.getRole() == Role.ADMIN && agentId != null) {
            return requireActiveAssignee(agentId);
        }
        if (!actor.isActive()) {
            throw ApiException.forbidden("You do not have access to this lead");
        }
        return actor;
    }

    private UserAccount requireActiveAssignee(Long agentId) {
        UserAccount assignee = users.findById(agentId).orElseThrow(() -> ApiException.notFound("Assigned user not found"));
        if (!assignee.isActive()) {
            throw ApiException.badRequest("Leads can only be assigned to an active user");
        }
        return assignee;
    }

    private void ensureMutable(Lead lead) {
        if (lead.isArchived()) {
            throw ApiException.badRequest("Archived leads cannot be changed. Restore the lead first.");
        }
    }
}
