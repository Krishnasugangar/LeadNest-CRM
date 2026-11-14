package com.leadnest.crm.service;

import com.leadnest.crm.domain.DealStage;
import com.leadnest.crm.domain.LeadStatus;
import com.leadnest.crm.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolicyTest {

    @Test
    void leadTransitionsFollowTheAllowedPath() {
        assertDoesNotThrow(() -> LeadStatusPolicy.assertCanChange(LeadStatus.NEW, LeadStatus.CONTACTED, false, null));
        assertDoesNotThrow(() -> LeadStatusPolicy.assertCanChange(LeadStatus.CONTACTED, LeadStatus.QUALIFIED, false, null));
        assertDoesNotThrow(() -> LeadStatusPolicy.assertCanChange(LeadStatus.QUALIFIED, LeadStatus.LOST, false, "No budget"));
        assertDoesNotThrow(() -> LeadStatusPolicy.assertCanChange(LeadStatus.NEW, LeadStatus.LOST, false, "Wrong fit"));

        ApiException skip = assertThrows(ApiException.class,
                () -> LeadStatusPolicy.assertCanChange(LeadStatus.NEW, LeadStatus.QUALIFIED, false, null));
        assertEquals(400, skip.getStatus().value());

        ApiException missingReason = assertThrows(ApiException.class,
                () -> LeadStatusPolicy.assertCanChange(LeadStatus.CONTACTED, LeadStatus.LOST, false, " "));
        assertEquals(400, missingReason.getStatus().value());

        ApiException dealBlocksLost = assertThrows(ApiException.class,
                () -> LeadStatusPolicy.assertCanChange(LeadStatus.QUALIFIED, LeadStatus.LOST, true, "Changed mind"));
        assertEquals(409, dealBlocksLost.getStatus().value());
    }

    @Test
    void dealStagesAreSequentialAndLostNeedsAReason() {
        assertDoesNotThrow(() -> DealStagePolicy.assertCanChange(DealStage.PROPOSAL, DealStage.NEGOTIATION, null));
        assertDoesNotThrow(() -> DealStagePolicy.assertCanChange(DealStage.NEGOTIATION, DealStage.WON, null));
        assertDoesNotThrow(() -> DealStagePolicy.assertCanChange(DealStage.PROPOSAL, DealStage.LOST, "Price"));

        ApiException skipWon = assertThrows(ApiException.class,
                () -> DealStagePolicy.assertCanChange(DealStage.PROPOSAL, DealStage.WON, null));
        assertEquals(400, skipWon.getStatus().value());

        ApiException missingReason = assertThrows(ApiException.class,
                () -> DealStagePolicy.assertCanChange(DealStage.NEGOTIATION, DealStage.LOST, null));
        assertEquals(400, missingReason.getStatus().value());

        ApiException terminal = assertThrows(ApiException.class,
                () -> DealStagePolicy.assertCanChange(DealStage.WON, DealStage.LOST, "Changed"));
        assertEquals(400, terminal.getStatus().value());
    }

    @Test
    void contactRulesRequireOneValidMethod() {
        assertEquals("a@b.co", ContactRules.parse(" A@b.co ", null).email());
        assertThrows(ApiException.class, () -> ContactRules.parse(null, null));
        assertThrows(ApiException.class, () -> ContactRules.parse("not-an-email", null));
        assertThrows(ApiException.class, () -> ContactRules.parse(null, "123"));
    }
}
