package com.leadnest.crm.config;

import com.leadnest.crm.domain.Role;
import com.leadnest.crm.domain.UserAccount;
import com.leadnest.crm.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${leadnest.admin.email:}") String email,
            @Value("${leadnest.admin.password:}") String password,
            @Value("${leadnest.admin.name:Administrator}") String name) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank()) {
            if (!users.existsByRole(Role.ADMIN)) {
                log.warn("No administrator exists. Set LEADNEST_ADMIN_EMAIL and LEADNEST_ADMIN_PASSWORD before the first start.");
            }
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalized)) {
            log.info("Bootstrap admin already present for {}", normalized);
            return;
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException("LEADNEST_ADMIN_PASSWORD must be 8 to 72 characters when LEADNEST_ADMIN_EMAIL is set");
        }
        UserAccount admin = new UserAccount();
        admin.setName(name == null || name.isBlank() ? "Administrator" : name.trim());
        admin.setEmail(normalized);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        users.save(admin);
        log.info("Created bootstrap administrator {}", normalized);
    }
}
