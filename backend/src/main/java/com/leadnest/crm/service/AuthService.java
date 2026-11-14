package com.leadnest.crm.service;

import com.leadnest.crm.domain.UserAccount;
import com.leadnest.crm.dto.ApiDtos.LoginRequest;
import com.leadnest.crm.dto.ApiDtos.MessageResponse;
import com.leadnest.crm.dto.ApiDtos.PasswordChangeRequest;
import com.leadnest.crm.dto.ApiDtos.UserResponse;
import com.leadnest.crm.exception.ApiException;
import com.leadnest.crm.repository.UserAccountRepository;
import com.leadnest.crm.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final CurrentUserService currentUser;
    private final UserAccountRepository users;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthService(
            AuthenticationManager authenticationManager,
            CurrentUserService currentUser,
            UserAccountRepository users,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.currentUser = currentUser;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            httpRequest.getSession(true);
            httpRequest.changeSessionId();
            securityContextRepository.saveContext(context, httpRequest, httpResponse);
            return UserResponse.from(((AppUserDetails) authentication.getPrincipal()).getUser());
        } catch (DisabledException exception) {
            throw ApiException.unauthorized("This account is deactivated");
        } catch (AuthenticationException exception) {
            throw ApiException.unauthorized("Invalid email or password");
        }
    }

    public UserResponse current() {
        return UserResponse.from(currentUser.require());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }

    @Transactional
    public MessageResponse changePassword(PasswordChangeRequest request) {
        UserAccount user = currentUser.require();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw ApiException.badRequest("Choose a password that is different from the current one");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        return new MessageResponse("Password updated");
    }
}
