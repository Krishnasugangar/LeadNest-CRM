package com.leadnest.crm.repository;

import com.leadnest.crm.domain.FollowUp;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long>, JpaSpecificationExecutor<FollowUp> {

    @Query("""
            select f from FollowUp f
            join fetch f.createdBy
            where f.lead.id = :leadId
            order by f.dueAt asc
            """)
    List<FollowUp> findForLead(@Param("leadId") Long leadId);

    @Query("""
            select f from FollowUp f
            join fetch f.lead l
            join fetch f.createdBy
            where f.completed = false
              and f.dueAt >= :now
              and l.archived = false
              and (:admin = true or l.assignedAgent.id = :agentId)
            order by f.dueAt asc
            """)
    List<FollowUp> findUpcoming(
            @Param("now") Instant now,
            @Param("admin") boolean admin,
            @Param("agentId") Long agentId,
            Pageable pageable);

    @Query("""
            select count(f) from FollowUp f
            where f.completed = false
              and f.dueAt < :now
              and f.lead.archived = false
              and (:admin = true or f.lead.assignedAgent.id = :agentId)
            """)
    long countOverdue(@Param("now") Instant now, @Param("admin") boolean admin, @Param("agentId") Long agentId);
}
