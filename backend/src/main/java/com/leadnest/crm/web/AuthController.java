package com.leadnest.crm.web;

import com.leadnest.crm.dto.ApiDtos.LoginRequest;
import com.leadnest.crm.dto.ApiDtos.MessageResponse;
import com.leadnest.crm.dto.ApiDtos.PasswordChangeRequest;
import com.leadnest.crm.dto.ApiDtos.UserResponse;
import com.leadnest.crm.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return authService.login(request, httpRequest, httpResponse);
    }

    @PostMapping("/logout")
    public MessageResponse logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return new MessageResponse("Signed out");
    }

    @GetMapping("/me")
    public UserResponse me() {
        return authService.current();
    }

    @PutMapping("/password")
    public MessageResponse changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        return authService.changePassword(request);
    }
}
