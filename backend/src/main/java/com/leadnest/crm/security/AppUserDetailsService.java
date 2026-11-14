package com.leadnest.crm.security;

import com.leadnest.crm.domain.UserAccount;
import com.leadnest.crm.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserAccountRepository users;

    public AppUserDetailsService(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserAccount user = users.findByEmailIgnoreCase(username.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        return new AppUserDetails(user);
    }
}
