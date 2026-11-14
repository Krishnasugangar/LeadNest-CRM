package com.leadnest.crm.service;

import com.leadnest.crm.domain.Deal;
import com.leadnest.crm.domain.DealStage;
import com.leadnest.crm.domain.HistoryEventType;
import com.leadnest.crm.domain.Lead;
import com.leadnest.crm.domain.LeadStatus;
import com.leadnest.crm.domain.Role;
import com.leadnest.crm.domain.UserAccount;
import com.leadnest.crm.dto.ApiDtos.DealResponse;
import com.leadnest.crm.dto.ApiDtos.DealUpdateRequest;
import com.leadnest.crm.dto.ApiDtos.DealWriteRequest;
import com.leadnest.crm.dto.ApiDtos.PageResponse;
import com.leadnest.crm.dto.ApiDtos.StageChangeRequest;
import com.leadnest.crm.exception.ApiException;
import com.leadnest.crm.repository.DealRepository;
import com.leadnest.crm.support.PageRequests;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class DealService {

    private final DealRepository deals;
    private final AccessGuard access;
    private final HistoryRecorder history;

    public DealService(DealRepository deals, AccessGuard access, HistoryRecorder history) {
        this.deals = deals;
        this.access = access;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public PageResponse<DealResponse> search(DealStage stage, int page, int size) {
        UserAccount actor = access.actor();
        Page<DealResponse> result = deals.findAll(scoped(actor, stage), PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(DealResponse::from);
        return PageRequests.toResponse(result);
    }

    @Transactional(readOnly = true)
    public DealResponse get(Long id) {
        return DealResponse.from(access.requireDeal(id));
    }

    @Transactional
    public DealResponse convert(Long leadId, DealWriteRequest request) {
        UserAccount actor = access.actor();
        Lead lead = access.requireLeadForUpdate(leadId);
        if (lead.isArchived()) {
            throw ApiException.badRequest("Archived leads cannot be converted");
        }
        if (lead.getStatus() != LeadStatus.QUALIFIED) {
            throw ApiException.badRequest("Only a qualified lead can be converted to a deal");
        }
        if (deals.existsByLeadId(leadId)) {
            throw ApiException.conflict("This lead already has a deal");
        }
        Deal deal = new Deal();
        deal.setLead(lead);
        deal.setTitle(request.title().trim());
        deal.setAmount(money(request.amount()));
        deal.setStage(DealStage.PROPOSAL);
        deal.setExpectedCloseDate(request.expectedCloseDate());
        try {
            Deal saved = deals.saveAndFlush(deal);
            history.record(lead, actor, HistoryEventType.CONVERTED, "Converted to deal: " + saved.getTitle());
            return DealResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict("This lead already has a deal");
        }
    }

    @Transactional
    public DealResponse update(Long id, DealUpdateRequest request) {
        Deal deal = access.requireDealForUpdate(id);
        ensureOpen(deal);
        deal.setTitle(request.title().trim());
        deal.setAmount(money(request.amount()));
        deal.setExpectedCloseDate(request.expectedCloseDate());
        return DealResponse.from(deal);
    }

    @Transactional
    public DealResponse changeStage(Long id, StageChangeRequest request) {
        UserAccount actor = access.actor();
        Deal deal = access.requireDealForUpdate(id);
        if (deal.getLead().isArchived()) {
            throw ApiException.badRequest("Deals on archived leads cannot change stage");
        }
        String reason = ContactRules.blankToNull(request.closingReason());
        DealStagePolicy.assertCanChange(deal.getStage(), request.stage(), reason);
        DealStage previous = deal.getStage();
        deal.setStage(request.stage());
        deal.setClosingReason(request.stage() == DealStage.LOST ? reason : reason);
        if (request.stage() != DealStage.LOST && reason == null) {
            deal.setClosingReason(null);
        }
        String detail = "Deal moved from " + previous + " to " + request.stage();
        if (request.stage() == DealStage.LOST) {
            detail = detail + ". Reason: " + reason;
        }
        history.record(deal.getLead(), actor, HistoryEventType.DEAL_STAGE_CHANGED, detail);
        return DealResponse.from(deal);
    }

    private void ensureOpen(Deal deal) {
        if (deal.getLead().isArchived()) {
            throw ApiException.badRequest("Deals on archived leads cannot be edited");
        }
        if (deal.getStage() == DealStage.WON || deal.getStage() == DealStage.LOST) {
            throw ApiException.badRequest("Closed deals cannot be edited");
        }
    }

    private Specification<Deal> scoped(UserAccount actor, DealStage stage) {
        return (root, query, cb) -> {
            boolean countQuery = Long.class.equals(query.getResultType()) || long.class.equals(query.getResultType());
            Join<Object, Object> lead;
            if (countQuery) {
                lead = root.join("lead", JoinType.INNER);
            } else {
                Fetch<Object, Object> leadFetch = root.fetch("lead", JoinType.INNER);
                leadFetch.fetch("assignedAgent", JoinType.INNER);
                lead = (Join<Object, Object>) leadFetch;
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (actor.getRole() != Role.ADMIN) {
                predicates.add(cb.equal(lead.get("assignedAgent").get("id"), actor.getId()));
            }
            if (stage != null) {
                predicates.add(cb.equal(root.get("stage"), stage));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
