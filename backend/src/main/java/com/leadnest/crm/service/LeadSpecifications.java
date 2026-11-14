package com.leadnest.crm.service;

import com.leadnest.crm.domain.Lead;
import com.leadnest.crm.domain.LeadStatus;
import com.leadnest.crm.domain.Role;
import com.leadnest.crm.domain.UserAccount;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class LeadSpecifications {

    private LeadSpecifications() {
    }

    public static Specification<Lead> matching(
            UserAccount actor,
            String search,
            LeadStatus status,
            Long agentId,
            boolean includeArchived) {
        return (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("assignedAgent", JoinType.INNER);
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (!includeArchived) {
                predicates.add(cb.isFalse(root.get("archived")));
            }
            if (actor.getRole() != Role.ADMIN) {
                predicates.add(cb.equal(root.get("assignedAgent").get("id"), actor.getId()));
            } else if (agentId != null) {
                predicates.add(cb.equal(root.get("assignedAgent").get("id"), agentId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("email"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), like)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
