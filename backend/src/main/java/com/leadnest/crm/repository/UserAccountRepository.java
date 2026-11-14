package com.leadnest.crm.repository;

import com.leadnest.crm.domain.Role;
import com.leadnest.crm.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);

    List<UserAccount> findAllByOrderByNameAsc();
}
