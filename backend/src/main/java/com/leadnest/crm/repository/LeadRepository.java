package com.leadnest.crm.repository;

import com.leadnest.crm.domain.Lead;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lead l where l.id = :id")
    Optional<Lead> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select count(l) from Lead l
            where l.archived = false
              and l.status <> com.leadnest.crm.domain.LeadStatus.LOST
              and (:admin = true or l.assignedAgent.id = :agentId)
            """)
    long countActive(@Param("admin") boolean admin, @Param("agentId") Long agentId);

    @Query("""
            select count(l) from Lead l
            where l.archived = false
              and l.status = com.leadnest.crm.domain.LeadStatus.QUALIFIED
              and (:admin = true or l.assignedAgent.id = :agentId)
            """)
    long countQualified(@Param("admin") boolean admin, @Param("agentId") Long agentId);

    @Query("""
            select l.status, count(l) from Lead l
            where l.archived = false
              and (:admin = true or l.assignedAgent.id = :agentId)
            group by l.status
            """)
    List<Object[]> countByStatus(@Param("admin") boolean admin, @Param("agentId") Long agentId);

    @Query("""
            select case when count(l) > 0 then true else false end
            from Lead l
            where l.assignedAgent.id = :agentId
            """)
    boolean existsAssignedTo(@Param("agentId") Long agentId);
}
