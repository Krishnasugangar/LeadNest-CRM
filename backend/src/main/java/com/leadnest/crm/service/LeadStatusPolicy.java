package com.leadnest.crm.service;

import com.leadnest.crm.domain.LeadStatus;
import com.leadnest.crm.exception.ApiException;

public final class LeadStatusPolicy {

    private LeadStatusPolicy() {
    }

    public static void assertCanChange(LeadStatus current, LeadStatus next, boolean dealExists, String lostReason) {
        if (current == LeadStatus.QUALIFIED && next == LeadStatus.LOST && dealExists) {
            throw ApiException.conflict("A qualified lead with a deal cannot be marked lost. Close the deal instead.");
        }
        boolean allowed = switch (current) {
            case NEW -> next == LeadStatus.CONTACTED || next == LeadStatus.LOST;
            case CONTACTED -> next == LeadStatus.QUALIFIED || next == LeadStatus.LOST;
            case QUALIFIED -> next == LeadStatus.LOST;
            case LOST -> false;
        };
        if (!allowed) {
            throw ApiException.badRequest("Cannot change a lead from " + current + " to " + next);
        }
        if (next == LeadStatus.LOST && (lostReason == null || lostReason.isBlank())) {
            throw ApiException.badRequest("A reason is required when marking a lead lost");
        }
    }
}
