package com.swk.claimhelpers.user.service;

import com.swk.claimhelpers.common.exception.CustomException;
import com.swk.claimhelpers.common.exception.ErrorCode;
import com.swk.claimhelpers.user.entity.User;
import com.swk.claimhelpers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_ERROR));
    }
}