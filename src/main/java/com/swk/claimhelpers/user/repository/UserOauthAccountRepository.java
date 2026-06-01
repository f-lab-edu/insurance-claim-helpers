package com.swk.claimhelpers.user.repository;

import com.swk.claimhelpers.user.entity.UserOauthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface UserOauthAccountRepository extends JpaRepository<UserOauthAccount, Long> {

    // 프로바이더 + 키로 계정 조회 (로그인 시 사용)
    Optional<UserOauthAccount> findByProviderAndOauthKey(String provider, String oauthKey);

    // 특정 사용자의 OAuth 계정 목록 조회
    List<UserOauthAccount> findByUserId(Long userId);
}