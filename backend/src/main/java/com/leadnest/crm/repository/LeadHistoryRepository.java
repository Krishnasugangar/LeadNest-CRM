package com.leadnest.crm.repository;

import com.leadnest.crm.domain.LeadHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LeadHistoryRepository extends JpaRepository<LeadHistory, Long> {

    @Query("""
            select h from LeadHistory h
            join fetch h.actor
            where h.lead.id = :leadId
            order by h.createdAt asc
            """)
    List<LeadHistory> findForLead(@Param("leadId") Long leadId);
}
