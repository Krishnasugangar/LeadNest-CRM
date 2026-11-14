package com.leadnest.crm.web;

import com.leadnest.crm.dto.ApiDtos.ActiveStatusRequest;
import com.leadnest.crm.dto.ApiDtos.CreateAgentRequest;
import com.leadnest.crm.dto.ApiDtos.UserResponse;
import com.leadnest.crm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final UserService userService;

    public AgentController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.listAgents();
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateAgentRequest request) {
        return userService.createAgent(request);
    }

    @PatchMapping("/{id}/active")
    public UserResponse setActive(@PathVariable Long id, @Valid @RequestBody ActiveStatusRequest request) {
        return userService.setActive(id, request);
    }
}
