package com.swk.claimhelpers.chat.controller;

import com.swk.claimhelpers.chat.dto.ChatSessionCreateResponse;
import com.swk.claimhelpers.chat.dto.ChatSessionDetailResponse;
import com.swk.claimhelpers.chat.dto.ChatSessionListResponse;
import com.swk.claimhelpers.chat.service.ChatSessionService;
import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import com.swk.claimhelpers.common.util.SessionKeyResolver;
import com.swk.claimhelpers.user.entity.User;
import com.swk.claimhelpers.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;
    private final UserService userService;
    private final SessionKeyResolver sessionKeyResolver;

    @PostMapping
    public ResponseEntity<ChatSessionCreateResponse> create(
            @AuthenticationPrincipal OidcUser principal,
            HttpServletRequest request) {

        Owner owner = resolveOwner(principal, request);
        ChatSessionCreateResponse response = chatSessionService.create(owner.user(), owner.sessionKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ChatSessionListResponse> list(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        User user = userService.getByEmail(principal.getEmail());
        return chatSessionService.findList(user.getId());
    }

    @GetMapping("/{sessionId}")
    public ChatSessionDetailResponse detail(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal OidcUser principal,
            HttpServletRequest request) {

        Owner owner = resolveOwner(principal, request);
        return chatSessionService.findDetail(sessionId, owner.user(), owner.sessionKey());
    }

    private Owner resolveOwner(OidcUser principal, HttpServletRequest request) {
        if (principal != null) {
            User user = userService.getByEmail(principal.getEmail());
            return new Owner(user, null);
        }
        return new Owner(null, sessionKeyResolver.resolve(request));
    }

    private record Owner(User user, String sessionKey) {
    }
}