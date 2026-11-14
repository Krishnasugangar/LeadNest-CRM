package com.leadnest.crm.repository;

import com.leadnest.crm.domain.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @Query("""
            select a from Activity a
            join fetch a.author
            where a.lead.id = :leadId
            order by a.createdAt desc
            """)
    List<Activity> findForLead(@Param("leadId") Long leadId);
}
