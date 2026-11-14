package com.leadnest.crm.config;

import com.leadnest.crm.domain.Lead;
import com.leadnest.crm.domain.LeadSource;
import com.leadnest.crm.domain.LeadStatus;
import com.leadnest.crm.domain.Role;
import com.leadnest.crm.domain.UserAccount;
import com.leadnest.crm.repository.LeadRepository;
import com.leadnest.crm.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@ConditionalOnProperty(name = "leadnest.demo-data", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final UserAccountRepository users;
    private final LeadRepository leads;

    public DemoDataLoader(UserAccountRepository users, LeadRepository leads) {
        this.users = users;
        this.leads = leads;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (leads.count() > 0) {
            return;
        }
        UserAccount owner = users.findAllByOrderByNameAsc().stream()
                .filter(user -> user.getRole() == Role.ADMIN && user.isActive())
                .findFirst()
                .orElse(null);
        if (owner == null) {
            log.warn("Demo data skipped because no active administrator exists yet");
            return;
        }
        save(owner, "Northwind Studios", "priya@northwind.example", "9845011122", "Northwind", LeadSource.WEBSITE, LeadStatus.NEW, "Asked for a walkthrough of the annual plan.");
        save(owner, "Harbor and Co", null, "08041239876", "Harbor and Co", LeadSource.REFERRAL, LeadStatus.CONTACTED, "Referred by an existing customer.");
        save(owner, "Kalpa Retail", "hello@kalpa.example", null, "Kalpa Retail", LeadSource.SOCIAL_MEDIA, LeadStatus.QUALIFIED, "Ready for a proposal next week.");
        log.info("Loaded fictional demo leads for {}", owner.getEmail());
    }

    private void save(UserAccount owner, String name, String email, String phone, String company, LeadSource source, LeadStatus status, String notes) {
        Lead lead = new Lead();
        lead.setName(name);
        lead.setEmail(email);
        lead.setPhone(phone);
        lead.setCompany(company);
        lead.setSource(source);
        lead.setStatus(status);
        lead.setAssignedAgent(owner);
        lead.setNotes(notes);
        lead.setArchived(false);
        leads.save(lead);
    }
}
