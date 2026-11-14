package com.leadnest.crm.service;

import com.leadnest.crm.domain.UserAccount;
import com.leadnest.crm.exception.ApiException;
import com.leadnest.crm.repository.UserAccountRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserAccountRepository users;

    public CurrentUserService(UserAccountRepository users) {
        this.users = users;
    }

    public UserAccount require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw ApiException.unauthorized("Authentication required");
        }
        return users.findByEmailIgnoreCase(authentication.getName())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> ApiException.unauthorized("Authentication required"));
    }
}
