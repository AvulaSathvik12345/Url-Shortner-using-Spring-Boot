package com.example.urlshortner.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Skip rate limiting for static resources
        if (path.equals("/") || path.endsWith(".html") ||
                path.endsWith(".css") || path.endsWith(".js")) {
            chain.doFilter(request, response);
            return;
        }
        String ip = request.getRemoteAddr();
        TokenBucket bucket = buckets.computeIfAbsent(ip, k -> new TokenBucket(10, 1)); // 10 tokens, refill 1/sec

        if (!bucket.tryConsume()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    static class TokenBucket {
        private final int capacity;
        private final double refillRate;     // tokens per second
        private double tokens;
        private long lastRefillTime;

        TokenBucket(int capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1) { tokens--; return true; }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double elapsed = (now - lastRefillTime) / 1000.0;
            tokens = Math.min(capacity, tokens + elapsed * refillRate);
            lastRefillTime = now;
        }
    }
}