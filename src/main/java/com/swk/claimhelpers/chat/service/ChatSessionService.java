package com.swk.claimhelpers.chat.service;

import com.swk.claimhelpers.chat.dto.ChatMessageResponse;
import com.swk.claimhelpers.chat.dto.ChatSessionCreateResponse;
import com.swk.claimhelpers.chat.dto.ChatSessionDetailResponse;
import com.swk.claimhelpers.chat.dto.ChatSessionListResponse;
import com.swk.claimhelpers.chat.dto.ClaimCriteriaDetail;
import com.swk.claimhelpers.chat.dto.ClaimCriteriaSummary;
import com.swk.claimhelpers.chat.entity.ChatMessage;
import com.swk.claimhelpers.chat.entity.ChatSession;
import com.swk.claimhelpers.chat.entity.ChatSessionClaimCriteria;
import com.swk.claimhelpers.chat.repository.ChatMessageRepository;
import com.swk.claimhelpers.chat.repository.ChatSessionClaimCriteriaRepository;
import com.swk.claimhelpers.chat.repository.ChatSessionRepository;
import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import com.swk.claimhelpers.policy.entity.Document;
import com.swk.claimhelpers.policy.repository.DocumentRepository;
import com.swk.claimhelpers.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatSessionClaimCriteriaRepository chatSessionClaimCriteriaRepository;
    private final DocumentRepository documentRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatSessionCreateResponse create(User user, String sessionKey) {
        ChatSession session = (user != null)
                ? ChatSession.createForUser(user)
                : ChatSession.createForSession(sessionKey);
        ChatSession saved = chatSessionRepository.save(session);
        return new ChatSessionCreateResponse(saved.getId(), List.of(), saved.getCreatedAt());
    }

    /**
     * @param user       로그인 사용자(없으면 null)
     * @param sessionKey 비로그인 식별 키(로그인 시 null)
     */
    @Transactional(readOnly = true)
    public ChatSession findOwned(Long id, User user, String sessionKey) {
        ChatSession session = chatSessionRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        if (user != null) {
            User owner = session.getUser();
            if (owner == null || !Objects.equals(owner.getId(), user.getId())) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
        } else {
            if (!Objects.equals(session.getSessionKey(), sessionKey)) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
        }

        return session;
    }

    @Transactional(readOnly = true)
    public List<ChatSessionListResponse> findList(Long userId) {
        List<ChatSession> sessions = chatSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (sessions.isEmpty()) {
            return List.of();
        }
        List<Long> sessionIds = sessions.stream().map(ChatSession::getId).toList();

        List<ChatSessionClaimCriteria> links =
                chatSessionClaimCriteriaRepository.findByChatSessionIdInFetchClaimCriteria(sessionIds);
        Map<Long,String> fileNameByCriteriaId = fileNameMap(links);

        Map<Long,List<ClaimCriteriaSummary>> summariesBySession = links.stream()
                .collect(Collectors.groupingBy(
                        link -> link.getChatSession().getId(),
                        Collectors.mapping(link -> new ClaimCriteriaSummary(
                                        link.getClaimCriteria().getId(),
                                        fileNameByCriteriaId.get(link.getClaimCriteria().getId())),
                                Collectors.toList())));

        Map<Long,String> lastMessageBySession =
                chatMessageRepository.findLastMessagesByChatSessionIds(sessionIds).stream()
                        .collect(Collectors.toMap(
                                message -> message.getChatSession().getId(),
                                ChatMessage::getContent));

        return sessions.stream()
                .map(session -> new ChatSessionListResponse(
                        session.getId(),
                        summariesBySession.getOrDefault(session.getId(), List.of()),
                        lastMessageBySession.get(session.getId()),
                        session.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatSessionDetailResponse findDetail(Long id, User user, String sessionKey) {
        ChatSession session = findOwned(id, user, sessionKey);

        List<ChatSessionClaimCriteria> links =
                chatSessionClaimCriteriaRepository.findByChatSessionIdInFetchClaimCriteria(List.of(id));
        Map<Long,String> fileNameByCriteriaId = fileNameMap(links);

        List<ClaimCriteriaDetail> claimCriteria = links.stream()
                .map(link -> new ClaimCriteriaDetail(
                        link.getClaimCriteria().getId(),
                        fileNameByCriteriaId.get(link.getClaimCriteria().getId()),
                        link.getClaimCriteria().getStatus()))
                .toList();

        List<ChatMessageResponse> messages =
                chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(id).stream()
                        .map(ChatMessageResponse::from)
                        .toList();

        return new ChatSessionDetailResponse(session.getId(), claimCriteria, messages);
    }

    private Map<Long, String> fileNameMap(List<ChatSessionClaimCriteria> links) {
        List<Long> criteriaIds = links.stream()
                .map(link -> link.getClaimCriteria().getId())
                .toList();
        if (criteriaIds.isEmpty()) {
            return Map.of();
        }
        return documentRepository.findByClaimCriteriaIdIn(criteriaIds).stream()
                .collect(Collectors.toMap(
                        document -> document.getClaimCriteria().getId(),
                        Document::getFileName));
    }
}