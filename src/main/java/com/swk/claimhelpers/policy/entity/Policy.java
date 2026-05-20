package com.swk.claimhelpers.policy.entity;

import com.swk.claimhelpers.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Policy {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // nullable — 비로그인 사용자는 user_id 없음
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "session_key", length = 255)
    private String sessionKey;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PolicyStatus status;

    // V7에서 추가된 nullable 컬럼
    @Column(name = "file_path", length = 500)
    private String filePath;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Policy createForUser(User user, String fileName, Long fileSize) {
        Policy policy = new Policy();
        policy.user = user;
        policy.fileName = fileName;
        policy.fileSize = fileSize;
        policy.status = PolicyStatus.PENDING;
        return policy;
    }

    public static Policy createForSession(String sessionKey, String fileName, Long fileSize) {
        Policy policy = new Policy();
        policy.sessionKey = sessionKey;
        policy.fileName = fileName;
        policy.fileSize = fileSize;
        policy.status = PolicyStatus.PENDING;
        return policy;
    }

    public void updateStatus(PolicyStatus newStatus) {
        this.status = newStatus;
    }

    public void updateFilePath(String filePath) {
        this.filePath = filePath;
    }
}