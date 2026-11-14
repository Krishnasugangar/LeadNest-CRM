package com.leadnest.crm.web;

import com.leadnest.crm.domain.FollowUpView;
import com.leadnest.crm.dto.ApiDtos.FollowUpResponse;
import com.leadnest.crm.dto.ApiDtos.PageResponse;
import com.leadnest.crm.service.FollowUpService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follow-ups")
public class FollowUpController {

    private final FollowUpService followUpService;

    public FollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    @GetMapping
    public PageResponse<FollowUpResponse> search(
            @RequestParam(defaultValue = "PENDING") FollowUpView view,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return followUpService.search(view, page, size);
    }

    @PostMapping("/{id}/complete")
    public FollowUpResponse complete(@PathVariable Long id) {
        return followUpService.complete(id);
    }
}
