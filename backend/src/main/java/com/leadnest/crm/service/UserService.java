package com.leadnest.crm.service;

import com.leadnest.crm.domain.Role;
import com.leadnest.crm.domain.UserAccount;
import com.leadnest.crm.dto.ApiDtos.ActiveStatusRequest;
import com.leadnest.crm.dto.ApiDtos.CreateAgentRequest;
import com.leadnest.crm.dto.ApiDtos.UserResponse;
import com.leadnest.crm.exception.ApiException;
import com.leadnest.crm.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AccessGuard access;

    public UserService(UserAccountRepository users, PasswordEncoder passwordEncoder, AccessGuard access) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAgents() {
        access.requireAdmin();
        return users.findAllByOrderByNameAsc().stream()
                .filter(user -> user.getRole() == Role.AGENT)
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse createAgent(CreateAgentRequest request) {
        access.requireAdmin();
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with that email already exists");
        }
        UserAccount agent = new UserAccount();
        agent.setName(request.name().trim());
        agent.setEmail(email);
        agent.setPasswordHash(passwordEncoder.encode(request.password()));
        agent.setRole(Role.AGENT);
        agent.setActive(true);
        return UserResponse.from(users.save(agent));
    }

    @Transactional
    public UserResponse setActive(Long id, ActiveStatusRequest request) {
        UserAccount actor = access.actor();
        access.requireAdmin();
        UserAccount agent = users.findById(id).orElseThrow(() -> ApiException.notFound("Agent not found"));
        if (agent.getRole() != Role.AGENT) {
            throw ApiException.badRequest("Only agent accounts can be activated or deactivated");
        }
        if (agent.getId().equals(actor.getId())) {
            throw ApiException.badRequest("You cannot change your own access");
        }
        agent.setActive(request.active());
        return UserResponse.from(users.save(agent));
    }
}
