package com.leadnest.crm.service;

import com.leadnest.crm.domain.Deal;
import com.leadnest.crm.domain.FollowUp;
import com.leadnest.crm.domain.Lead;
import com.leadnest.crm.domain.Role;
import com.leadnest.crm.domain.UserAccount;
import com.leadnest.crm.exception.ApiException;
import com.leadnest.crm.repository.DealRepository;
import com.leadnest.crm.repository.FollowUpRepository;
import com.leadnest.crm.repository.LeadRepository;
import org.springframework.stereotype.Service;

@Service
public class AccessGuard {

    private final CurrentUserService currentUser;
    private final LeadRepository leads;
    private final DealRepository deals;
    private final FollowUpRepository followUps;

    public AccessGuard(
            CurrentUserService currentUser,
            LeadRepository leads,
            DealRepository deals,
            FollowUpRepository followUps) {
        this.currentUser = currentUser;
        this.leads = leads;
        this.deals = deals;
        this.followUps = followUps;
    }

    public UserAccount actor() {
        return currentUser.require();
    }

    public void requireAdmin() {
        if (actor().getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Admin access is required");
        }
    }

    public boolean isAdmin(UserAccount user) {
        return user.getRole() == Role.ADMIN;
    }

    public Lead requireLead(Long id) {
        Lead lead = leads.findById(id).orElseThrow(() -> ApiException.notFound("Lead not found"));
        assertLeadAccess(lead);
        return lead;
    }

    public Lead requireLeadForUpdate(Long id) {
        Lead lead = leads.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Lead not found"));
        assertLeadAccess(lead);
        return lead;
    }

    public Deal requireDeal(Long id) {
        Deal deal = deals.findById(id).orElseThrow(() -> ApiException.notFound("Deal not found"));
        assertLeadAccess(deal.getLead());
        return deal;
    }

    public Deal requireDealForUpdate(Long id) {
        Deal preview = deals.findById(id).orElseThrow(() -> ApiException.notFound("Deal not found"));
        Lead lead = leads.findByIdForUpdate(preview.getLead().getId())
                .orElseThrow(() -> ApiException.notFound("Lead not found"));
        assertLeadAccess(lead);
        return deals.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Deal not found"));
    }

    public FollowUp requireFollowUp(Long id) {
        FollowUp followUp = followUps.findById(id).orElseThrow(() -> ApiException.notFound("Follow-up not found"));
        assertLeadAccess(followUp.getLead());
        return followUp;
    }

    public void assertLeadAccess(Lead lead) {
        UserAccount actor = actor();
        if (actor.getRole() != Role.ADMIN && !lead.getAssignedAgent().getId().equals(actor.getId())) {
            throw ApiException.forbidden("You do not have access to this lead");
        }
    }
}
