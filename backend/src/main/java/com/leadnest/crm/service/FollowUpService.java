package com.leadnest.crm.service;

import com.leadnest.crm.domain.FollowUp;
import com.leadnest.crm.domain.FollowUpView;
import com.leadnest.crm.domain.Lead;
import com.leadnest.crm.domain.Role;
import com.leadnest.crm.domain.UserAccount;
import com.leadnest.crm.dto.ApiDtos.FollowUpRequest;
import com.leadnest.crm.dto.ApiDtos.FollowUpResponse;
import com.leadnest.crm.dto.ApiDtos.PageResponse;
import com.leadnest.crm.exception.ApiException;
import com.leadnest.crm.repository.FollowUpRepository;
import com.leadnest.crm.support.PageRequests;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class FollowUpService {

    private final FollowUpRepository followUps;
    private final AccessGuard access;

    public FollowUpService(FollowUpRepository followUps, AccessGuard access) {
        this.followUps = followUps;
        this.access = access;
    }

    @Transactional
    public FollowUpResponse create(Long leadId, FollowUpRequest request) {
        UserAccount actor = access.actor();
        Lead lead = access.requireLead(leadId);
        if (lead.isArchived()) {
            throw ApiException.badRequest("Archived leads cannot receive new follow-ups");
        }
        FollowUp followUp = new FollowUp();
        followUp.setLead(lead);
        followUp.setCreatedBy(actor);
        followUp.setDescription(request.description().trim());
        followUp.setDueAt(request.dueAt());
        followUp.setCompleted(false);
        return FollowUpResponse.from(followUps.save(followUp));
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUpResponse> search(FollowUpView view, int page, int size) {
        UserAccount actor = access.actor();
        FollowUpView selected = view == null ? FollowUpView.PENDING : view;
        Sort sort = selected == FollowUpView.COMPLETED
                ? Sort.by(Sort.Direction.DESC, "completedAt")
                : Sort.by(Sort.Direction.ASC, "dueAt");
        return PageRequests.toResponse(followUps.findAll(scoped(actor, selected, Instant.now()), PageRequests.of(page, size, sort))
                .map(FollowUpResponse::from));
    }

    @Transactional
    public FollowUpResponse complete(Long id) {
        FollowUp followUp = access.requireFollowUp(id);
        if (followUp.getLead().isArchived()) {
            throw ApiException.badRequest("Follow-ups on archived leads cannot be completed");
        }
        if (!followUp.isCompleted()) {
            followUp.setCompleted(true);
            followUp.setCompletedAt(Instant.now());
        }
        return FollowUpResponse.from(followUp);
    }

    private Specification<FollowUp> scoped(UserAccount actor, FollowUpView view, Instant now) {
        return (root, query, cb) -> {
            boolean countQuery = Long.class.equals(query.getResultType()) || long.class.equals(query.getResultType());
            Join<Object, Object> lead;
            if (countQuery) {
                lead = root.join("lead", JoinType.INNER);
            } else {
                Fetch<Object, Object> leadFetch = root.fetch("lead", JoinType.INNER);
                root.fetch("createdBy", JoinType.INNER);
                leadFetch.fetch("assignedAgent", JoinType.INNER);
                lead = (Join<Object, Object>) leadFetch;
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(lead.get("archived")));
            if (actor.getRole() != Role.ADMIN) {
                predicates.add(cb.equal(lead.get("assignedAgent").get("id"), actor.getId()));
            }
            if (view == FollowUpView.COMPLETED) {
                predicates.add(cb.isTrue(root.get("completed")));
            } else if (view == FollowUpView.OVERDUE) {
                predicates.add(cb.isFalse(root.get("completed")));
                predicates.add(cb.lessThan(root.get("dueAt"), now));
            } else {
                predicates.add(cb.isFalse(root.get("completed")));
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueAt"), now));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
