package com.swk.claimhelpers.user.repository;

import com.swk.claimhelpers.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}