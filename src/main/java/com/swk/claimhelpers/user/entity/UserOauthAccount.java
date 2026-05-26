package com.swk.claimhelpers.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_oauth_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOauthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // nullable 없음 — 반드시 user와 연결
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // OAuth 프로바이더 식별자 ('google', 'kakao' 등)
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    // 프로바이더가 발급한 subject ID (예: google_sub 값)
    @Column(name = "oauth_key", nullable = false, length = 255)
    private String oauthKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UserOauthAccount create(User user, String provider, String oauthKey) {
        UserOauthAccount account = new UserOauthAccount();
        account.user = user;
        account.provider = provider;
        account.oauthKey = oauthKey;
        return account;
    }
}