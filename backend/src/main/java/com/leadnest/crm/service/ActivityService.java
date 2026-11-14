package com.leadnest.crm.service;

import com.leadnest.crm.domain.Activity;
import com.leadnest.crm.domain.Lead;
import com.leadnest.crm.domain.UserAccount;
import com.leadnest.crm.dto.ApiDtos.ActivityRequest;
import com.leadnest.crm.dto.ApiDtos.ActivityResponse;
import com.leadnest.crm.exception.ApiException;
import com.leadnest.crm.repository.ActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

    private final ActivityRepository activities;
    private final AccessGuard access;

    public ActivityService(ActivityRepository activities, AccessGuard access) {
        this.activities = activities;
        this.access = access;
    }

    @Transactional
    public ActivityResponse add(Long leadId, ActivityRequest request) {
        UserAccount actor = access.actor();
        Lead lead = access.requireLead(leadId);
        if (lead.isArchived()) {
            throw ApiException.badRequest("Archived leads cannot receive new activities");
        }
        Activity activity = new Activity();
        activity.setLead(lead);
        activity.setAuthor(actor);
        activity.setType(request.type());
        activity.setDescription(request.description().trim());
        return ActivityResponse.from(activities.save(activity));
    }
}
