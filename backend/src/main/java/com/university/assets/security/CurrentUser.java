package com.university.assets.security;

import com.university.assets.common.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/** Static helper for reading the authenticated user from the security context. */
public final class CurrentUser {

    private CurrentUser() {}

    public static SecurityUser require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser user) {
            return user;
        }
        throw ApiException.unauthorized("Authentication required");
    }

    public static UUID id() {
        return require().getId();
    }

    public static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
