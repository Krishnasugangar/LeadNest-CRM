package com.leadnest.crm.web;

import com.leadnest.crm.domain.DealStage;
import com.leadnest.crm.dto.ApiDtos.DealResponse;
import com.leadnest.crm.dto.ApiDtos.DealUpdateRequest;
import com.leadnest.crm.dto.ApiDtos.PageResponse;
import com.leadnest.crm.dto.ApiDtos.StageChangeRequest;
import com.leadnest.crm.service.DealService;
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
@RequestMapping("/api/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    @GetMapping
    public PageResponse<DealResponse> search(
            @RequestParam(required = false) DealStage stage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return dealService.search(stage, page, size);
    }

    @GetMapping("/{id}")
    public DealResponse get(@PathVariable Long id) {
        return dealService.get(id);
    }

    @PutMapping("/{id}")
    public DealResponse update(@PathVariable Long id, @Valid @RequestBody DealUpdateRequest request) {
        return dealService.update(id, request);
    }

    @PostMapping("/{id}/stage")
    public DealResponse changeStage(@PathVariable Long id, @Valid @RequestBody StageChangeRequest request) {
        return dealService.changeStage(id, request);
    }
}
