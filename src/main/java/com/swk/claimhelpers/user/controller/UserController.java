package com.swk.claimhelpers.user.controller;

import com.swk.claimhelpers.user.dto.UserResponse;
import com.swk.claimhelpers.user.entity.User;
import com.swk.claimhelpers.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    
    @GetMapping("/api/users/me")
    public UserResponse getMe(@AuthenticationPrincipal OidcUser principal) {
        User user = userService.getByEmail(principal.getEmail());
        return UserResponse.from(user);
    }
}