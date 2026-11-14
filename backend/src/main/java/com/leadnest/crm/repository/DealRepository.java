package com.leadnest.crm.repository;

import com.leadnest.crm.domain.Deal;
import com.leadnest.crm.domain.DealStage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

public interface DealRepository extends JpaRepository<Deal, Long>, JpaSpecificationExecutor<Deal> {

    Optional<Deal> findByLeadId(Long leadId);

    boolean existsByLeadId(Long leadId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Deal d where d.id = :id")
    Optional<Deal> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select coalesce(sum(d.amount), 0) from Deal d
            where d.stage in :stages
              and d.lead.archived = false
              and (:admin = true or d.lead.assignedAgent.id = :agentId)
            """)
    BigDecimal sumAmount(
            @Param("stages") Collection<DealStage> stages,
            @Param("admin") boolean admin,
            @Param("agentId") Long agentId);
}
