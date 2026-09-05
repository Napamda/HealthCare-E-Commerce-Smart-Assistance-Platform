package org.example.Healthcareplatform.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {}

    /**
     * The JWT filter stores the authenticated user id (as a string) as the principal.
     */
    public static Long userId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long l) return l;
        try {
            return Long.valueOf(principal.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
