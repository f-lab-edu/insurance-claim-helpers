package com.swk.claimhelpers.common.web;

import com.swk.claimhelpers.user.entity.User;

public record Owner(User user, String sessionKey) {
}