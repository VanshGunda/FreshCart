package com.freshcart.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RequestRateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        int limit;
        Duration window;
        if (path.equals("/auth/login")) {
            limit = 10; window = Duration.ofMinutes(1);
        } else if (path.equals("/auth/reg")) {
            limit = 5; window = Duration.ofMinutes(10);
        } else if (path.equals("/auth/verify-otp")) {
            limit = 10; window = Duration.ofMinutes(10);
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        String key = path + "|" + request.getRemoteAddr();
        Instant now = Instant.now();
        if (windows.size() > 10000) cleanupExpiredWindows();
        Window current = windows.compute(key, (k, old) -> {
            if (old == null || Duration.between(old.startedAt, now).compareTo(window) >= 0) return new Window(now, 1);
            return new Window(old.startedAt, old.count + 1);
        });
        if (current.count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(Math.max(1, window.minus(Duration.between(current.startedAt, now)).toSeconds())));
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Scheduled(fixedDelayString = "${app.security.rate-limit-cleanup-ms:300000}")
    public void cleanupExpiredWindows() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(15));
        windows.entrySet().removeIf(e -> e.getValue().startedAt.isBefore(cutoff));
    }

    private record Window(Instant startedAt, int count) {}
}
