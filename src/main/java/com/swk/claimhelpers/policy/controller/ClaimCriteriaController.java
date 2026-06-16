package com.swk.claimhelpers.policy.controller;

import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import com.swk.claimhelpers.common.util.SessionKeyResolver;
import com.swk.claimhelpers.policy.dto.ClaimCriteriaUploadResponse;
import com.swk.claimhelpers.policy.entity.ClaimCriteria;
import com.swk.claimhelpers.policy.entity.ClaimCriteriaStatus;
import com.swk.claimhelpers.policy.entity.Document;
import com.swk.claimhelpers.policy.service.ClaimCriteriaProcessor;
import com.swk.claimhelpers.policy.service.ClaimCriteriaUploadService;
import com.swk.claimhelpers.user.entity.User;
import com.swk.claimhelpers.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/claim-criteria")
@RequiredArgsConstructor
public class ClaimCriteriaController {

    private final ClaimCriteriaUploadService uploadService;
    private final ClaimCriteriaProcessor processor;
    private final UserRepository userRepository;
    private final SessionKeyResolver sessionKeyResolver;

    /**
     * 약관 PDF 업로드.
     *
     * 동기 업로드(메타데이터 저장 + S3 업로드)가 커밋된 뒤 임베딩 작업을
     * 비동기 빈으로 넘긴다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClaimCriteriaUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal OidcUser principal,
            HttpServletRequest request) {

        // 로그인 사용자는 email 로 User 조회, 비로그인은 HTTP 세션 ID 를 식별자로 사용한다.
        User user = null;
        String sessionKey = null;
        if (principal != null) {
            user = userRepository.findByEmail(principal.getEmail())
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_ERROR));
        } else {
            sessionKey = sessionKeyResolver.resolve(request);
        }

        Document document = uploadService.upload(file, user, sessionKey);

        // 업로드 트랜잭션이 커밋된 이후 시점
        ClaimCriteria claimCriteria = document.getClaimCriteria();
        try {
            processor.process(claimCriteria.getId());
        } catch (TaskRejectedException e) {
            // 임베딩 풀+큐 포화로 작업이 거부
            log.warn("임베딩 작업 큐 포화로 거부됨: claimCriteriaId={}", claimCriteria.getId(), e);
            processor.markFailed(claimCriteria.getId());
            // 응답 DTO 에도 즉시 반영(detached 엔티티 필드만 갱신 — 영속화는 markFailed 가 담당)
            claimCriteria.updateStatus(ClaimCriteriaStatus.FAILED);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClaimCriteriaUploadResponse.from(claimCriteria, document));
    }
}