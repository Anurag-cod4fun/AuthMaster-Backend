package com.authutil.authmaster.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class CookieUtil {

    private static final boolean IS_DEV = true; // switch via env later

    public static ResponseCookie createRefreshCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(!IS_DEV)                    // true in production
                .sameSite(IS_DEV ? "Lax" : "None")  // None requires HTTPS
                .path("/api/auth/refresh")                 // apply to all auth routes
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    public static String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie c : request.getCookies()) {
            if ("refresh_token".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    public static ResponseCookie deleteRefreshCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(!IS_DEV)
                .sameSite(IS_DEV ? "Lax" : "None")
                .path("/api/auth/refresh")
                .maxAge(0)
                .build();
    }
}
