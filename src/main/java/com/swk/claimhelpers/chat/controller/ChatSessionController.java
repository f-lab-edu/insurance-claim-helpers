package com.swk.claimhelpers.chat.controller;

import com.swk.claimhelpers.chat.dto.ChatSessionCreateResponse;
import com.swk.claimhelpers.chat.dto.ChatSessionDetailResponse;
import com.swk.claimhelpers.chat.dto.ChatSessionListResponse;
import com.swk.claimhelpers.chat.dto.ClaimCriteriaAttachRequest;
import com.swk.claimhelpers.chat.dto.ClaimCriteriaAttachResponse;
import com.swk.claimhelpers.chat.service.ChatSessionService;
import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import com.swk.claimhelpers.common.web.Owner;
import com.swk.claimhelpers.user.entity.User;
import com.swk.claimhelpers.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ChatSessionCreateResponse> create(Owner owner) {
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
    public ChatSessionDetailResponse detail(@PathVariable Long sessionId, Owner owner) {
        return chatSessionService.findDetail(sessionId, owner.user(), owner.sessionKey());
    }

    @PostMapping("/{sessionId}/claim-criteria")
    public ClaimCriteriaAttachResponse attachClaimCriteria(
            @PathVariable Long sessionId,
            @RequestBody ClaimCriteriaAttachRequest request,
            Owner owner) {
        return chatSessionService.attachClaimCriteria(
                sessionId, request.claimCriteriaId(), owner.user(), owner.sessionKey());
    }

    @DeleteMapping("/{sessionId}/claim-criteria/{claimCriteriaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detachClaimCriteria(
            @PathVariable Long sessionId,
            @PathVariable Long claimCriteriaId,
            Owner owner) {
        chatSessionService.detachClaimCriteria(sessionId, claimCriteriaId, owner.user(), owner.sessionKey());
    }
}