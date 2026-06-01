package com.swk.claimhelpers.policy.entity;

import com.swk.claimhelpers.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "claim_criteria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // nullable — 비로그인 사용자는 user_id 없음
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "session_key", length = 255)
    private String sessionKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClaimCriteriaStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ClaimCriteria createForUser(User user) {
        ClaimCriteria criteria = new ClaimCriteria();
        criteria.user = user;
        criteria.status = ClaimCriteriaStatus.PENDING;
        return criteria;
    }

    public static ClaimCriteria createForSession(String sessionKey) {
        ClaimCriteria criteria = new ClaimCriteria();
        criteria.sessionKey = sessionKey;
        criteria.status = ClaimCriteriaStatus.PENDING;
        return criteria;
    }

    public void updateStatus(ClaimCriteriaStatus newStatus) {
        this.status = newStatus;
    }
}