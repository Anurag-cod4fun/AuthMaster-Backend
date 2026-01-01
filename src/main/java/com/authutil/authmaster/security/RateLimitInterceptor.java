package com.authutil.authmaster.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60000; // 1 minute

    private final ConcurrentHashMap<String, AttemptTracker> attempts = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String endpoint = request.getRequestURI();
        
        // Rate limit only auth endpoints
        if (!endpoint.matches("^/api/auth/(login|register)$")) {
            return true;
        }

        String clientIp = getClientIp(request);
        String key = clientIp + ":" + endpoint;

        AttemptTracker tracker = attempts.computeIfAbsent(key, k -> new AttemptTracker());

        if (tracker.isExpired()) {
            tracker.reset();
        }

        if (tracker.count.get() >= MAX_ATTEMPTS) {
            response.setStatus(429); // Too Many Requests
            response.getWriter().write("{\"error\": \"Too many requests. Try again later.\"}");
            return false;
        }

        tracker.count.incrementAndGet();
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class AttemptTracker {
        AtomicInteger count = new AtomicInteger(0);
        long firstAttemptTime = System.currentTimeMillis();

        boolean isExpired() {
            return System.currentTimeMillis() - firstAttemptTime > WINDOW_MS;
        }

        void reset() {
            count.set(0);
            firstAttemptTime = System.currentTimeMillis();
        }
    }
}
