package com.leadnest.crm.service;

import com.leadnest.crm.domain.HistoryEventType;
import com.leadnest.crm.domain.Lead;
import com.leadnest.crm.domain.LeadHistory;
import com.leadnest.crm.domain.UserAccount;
import com.leadnest.crm.repository.LeadHistoryRepository;
import org.springframework.stereotype.Service;

@Service
public class HistoryRecorder {

    private final LeadHistoryRepository history;

    public HistoryRecorder(LeadHistoryRepository history) {
        this.history = history;
    }

    public void record(Lead lead, UserAccount actor, HistoryEventType type, String detail) {
        LeadHistory event = new LeadHistory();
        event.setLead(lead);
        event.setActor(actor);
        event.setEventType(type);
        String text = detail == null ? "" : detail.trim();
        if (text.length() > 500) {
            text = text.substring(0, 500);
        }
        event.setDetail(text);
        history.save(event);
    }
}
