package com.leadnest.crm.web;

import com.leadnest.crm.domain.LeadStatus;
import com.leadnest.crm.dto.ApiDtos.ActivityRequest;
import com.leadnest.crm.dto.ApiDtos.ActivityResponse;
import com.leadnest.crm.dto.ApiDtos.AssignLeadRequest;
import com.leadnest.crm.dto.ApiDtos.DealResponse;
import com.leadnest.crm.dto.ApiDtos.DealWriteRequest;
import com.leadnest.crm.dto.ApiDtos.FollowUpRequest;
import com.leadnest.crm.dto.ApiDtos.FollowUpResponse;
import com.leadnest.crm.dto.ApiDtos.LeadDetailResponse;
import com.leadnest.crm.dto.ApiDtos.LeadResponse;
import com.leadnest.crm.dto.ApiDtos.LeadWriteRequest;
import com.leadnest.crm.dto.ApiDtos.PageResponse;
import com.leadnest.crm.dto.ApiDtos.StatusChangeRequest;
import com.leadnest.crm.service.ActivityService;
import com.leadnest.crm.service.DealService;
import com.leadnest.crm.service.FollowUpService;
import com.leadnest.crm.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;
    private final ActivityService activityService;
    private final FollowUpService followUpService;
    private final DealService dealService;

    public LeadController(
            LeadService leadService,
            ActivityService activityService,
            FollowUpService followUpService,
            DealService dealService) {
        this.leadService = leadService;
        this.activityService = activityService;
        this.followUpService = followUpService;
        this.dealService = dealService;
    }

    @GetMapping
    public PageResponse<LeadResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) Long agentId,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return leadService.search(search, status, agentId, includeArchived, page, size);
    }

    @PostMapping
    public LeadResponse create(@Valid @RequestBody LeadWriteRequest request) {
        return leadService.create(request);
    }

    @GetMapping("/{id}")
    public LeadDetailResponse get(@PathVariable Long id) {
        return leadService.get(id);
    }

    @PutMapping("/{id}")
    public LeadResponse update(@PathVariable Long id, @Valid @RequestBody LeadWriteRequest request) {
        return leadService.update(id, request);
    }

    @PostMapping("/{id}/status")
    public LeadResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        return leadService.changeStatus(id, request);
    }

    @PostMapping("/{id}/assign")
    public LeadResponse assign(@PathVariable Long id, @Valid @RequestBody AssignLeadRequest request) {
        return leadService.assign(id, request);
    }

    @PostMapping("/{id}/archive")
    public LeadResponse archive(@PathVariable Long id) {
        return leadService.archive(id);
    }

    @PostMapping("/{id}/restore")
    public LeadResponse restore(@PathVariable Long id) {
        return leadService.restore(id);
    }

    @PostMapping("/{id}/activities")
    public ActivityResponse addActivity(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
        return activityService.add(id, request);
    }

    @PostMapping("/{id}/follow-ups")
    public FollowUpResponse addFollowUp(@PathVariable Long id, @Valid @RequestBody FollowUpRequest request) {
        return followUpService.create(id, request);
    }

    @PostMapping("/{id}/deal")
    public DealResponse convert(@PathVariable Long id, @Valid @RequestBody DealWriteRequest request) {
        return dealService.convert(id, request);
    }
}
