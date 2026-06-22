package com.swk.claimhelpers.user.service;

import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import com.swk.claimhelpers.user.entity.User;
import com.swk.claimhelpers.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("이메일로 사용자를 찾으면 해당 User를 반환한다")
    void 이메일로_사용자를_찾으면_반환한다() {
        // given
        User user = User.create("a@gmail.com");
        given(userRepository.findByEmail("a@gmail.com")).willReturn(Optional.of(user));

        // when
        User result = userService.getByEmail("a@gmail.com");

        // then
        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("이메일로 사용자를 찾지 못하면 INTERNAL_ERROR 예외를 던진다")
    void 사용자를_찾지_못하면_예외를_던진다() {
        // given
        given(userRepository.findByEmail("none@gmail.com")).willReturn(Optional.empty());

        // when & then: CustomException(INTERNAL_ERROR)로 변환되어야 한다
        assertThatThrownBy(() -> userService.getByEmail("none@gmail.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INTERNAL_ERROR);
    }
}