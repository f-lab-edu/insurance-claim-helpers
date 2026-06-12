package com.swk.claimhelpers.user.service;

import com.swk.claimhelpers.user.entity.User;
import com.swk.claimhelpers.user.entity.UserOauthAccount;
import com.swk.claimhelpers.user.repository.UserOauthAccountRepository;
import com.swk.claimhelpers.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OAuthUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserOauthAccountRepository oauthAccountRepository;

    @InjectMocks
    private OAuthUserService oauthUserService;

    @Test
    @DisplayName("이미 가입한 OAuth 계정이면 연결된 User를 반환하고 새로 저장하지 않는다")
    void 기존_계정이면_연결된_User를_반환한다() {
        // given: (google, sub-123) 계정이 이미 존재하도록 스텁
        User existingUser = User.create("a@gmail.com");
        UserOauthAccount existingAccount = UserOauthAccount.create(existingUser, "google", "sub-123");
        given(oauthAccountRepository.findByProviderAndOauthKey("google", "sub-123"))
                .willReturn(Optional.of(existingAccount));

        // when
        User result = oauthUserService.findOrCreate("google", "sub-123", "a@gmail.com");

        // then: 기존 User가 그대로 반환되고, 저장(save)은 한 번도 호출되지 않아야 한다
        assertThat(result).isSameAs(existingUser);
        then(userRepository).should(never()).save(any());
        then(oauthAccountRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("가입 이력이 없으면 User와 UserOauthAccount를 새로 저장한다")
    void 신규_계정이면_User와_OAuth계정을_저장한다() {
        // given: (google, sub-999) 계정이 없도록 스텁
        given(oauthAccountRepository.findByProviderAndOauthKey("google", "sub-999"))
                .willReturn(Optional.empty());
        // userRepository.save가 저장된 User를 돌려주도록 스텁 (실제 save 동작 흉내)
        User savedUser = User.create("b@gmail.com");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        User result = oauthUserService.findOrCreate("google", "sub-999", "b@gmail.com");

        // then: 저장된 User가 반환되고, User·UserOauthAccount 둘 다 저장되어야 한다
        assertThat(result).isSameAs(savedUser);
        then(userRepository).should().save(any(User.class));
        then(oauthAccountRepository).should().save(any(UserOauthAccount.class));
    }
}