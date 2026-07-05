package com.swk.claimhelpers.common.web;

import com.swk.claimhelpers.common.util.SessionKeyResolver;
import com.swk.claimhelpers.user.entity.User;
import com.swk.claimhelpers.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class OwnerArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserService userService;
    private final SessionKeyResolver sessionKeyResolver;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(Owner.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser principal) {
            User user = userService.getByEmail(principal.getEmail());
            return new Owner(user, null);
        }

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        return new Owner(null, sessionKeyResolver.resolve(request));
    }
}