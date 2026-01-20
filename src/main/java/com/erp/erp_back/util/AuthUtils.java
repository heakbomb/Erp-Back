package com.erp.erp_back.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthUtils {

    private AuthUtils() {}

    /** SecurityContext에서 Authentication 꺼내기 */
    public static Authentication currentAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("Unauthenticated");
        return auth;
    }

    /** principal을 Long ID로 파싱 (너희 JwtAuthFilter에서 principal=subject(id) 넣는 구조 기준) */
    public static Long currentPrincipalId(Authentication authentication) {
        if (authentication == null) throw new IllegalStateException("Unauthenticated");
        Object principal = authentication.getPrincipal();
        if (principal == null) throw new IllegalStateException("Unauthenticated");

        String s = String.valueOf(principal).trim();
        if (s.isEmpty() || "anonymousUser".equalsIgnoreCase(s)) {
            throw new IllegalStateException("Unauthenticated");
        }

        try {
            long id = Long.parseLong(s);
            if (id <= 0) throw new IllegalArgumentException("Invalid principal id: " + id);
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid principal id: " + s, e);
        }
    }

    /** ROLE 체크 */
    public static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || role == null) return false;
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        for (GrantedAuthority a : authentication.getAuthorities()) {
            if (a != null && target.equals(a.getAuthority())) return true;
        }
        return false;
    }

    /** ownerId 강제 추출 (ROLE_OWNER 필수) */
    public static Long requireOwnerId(Authentication authentication) {
        if (!hasRole(authentication, "OWNER")) throw new IllegalStateException("Forbidden: OWNER role required");
        return currentPrincipalId(authentication);
    }

    /** employeeId 강제 추출 (ROLE_EMPLOYEE 필수) */
    public static Long requireEmployeeId(Authentication authentication) {
        if (!hasRole(authentication, "EMPLOYEE")) throw new IllegalStateException("Forbidden: EMPLOYEE role required");
        return currentPrincipalId(authentication);
    }

    /** adminId 강제 추출 (ROLE_ADMIN 필수) */
    public static Long requireAdminId(Authentication authentication) {
        if (!hasRole(authentication, "ADMIN")) throw new IllegalStateException("Forbidden: ADMIN role required");
        return currentPrincipalId(authentication);
    }
}