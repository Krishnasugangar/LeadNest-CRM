package com.leadnest.crm.service;

import com.leadnest.crm.domain.DealStage;
import com.leadnest.crm.exception.ApiException;

public final class DealStagePolicy {

    private DealStagePolicy() {
    }

    public static void assertCanChange(DealStage current, DealStage next, String closingReason) {
        if (current == DealStage.WON || current == DealStage.LOST) {
            throw ApiException.badRequest("A " + current + " deal cannot change stage");
        }
        boolean allowed = switch (current) {
            case PROPOSAL -> next == DealStage.NEGOTIATION || next == DealStage.LOST;
            case NEGOTIATION -> next == DealStage.WON || next == DealStage.LOST;
            case WON, LOST -> false;
        };
        if (!allowed) {
            throw ApiException.badRequest("Cannot change a deal from " + current + " to " + next);
        }
        if (next == DealStage.LOST && (closingReason == null || closingReason.isBlank())) {
            throw ApiException.badRequest("A reason is required when marking a deal lost");
        }
    }
}
