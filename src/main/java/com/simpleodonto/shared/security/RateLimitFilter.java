package com.simpleodonto.shared.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> apiBuckets  = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String ip  = resolveIp(req);
        String uri = req.getRequestURI();

        Bucket bucket = uri.startsWith("/api/auth")
                ? authBuckets.computeIfAbsent(ip, k -> authBucket())
                : apiBuckets.computeIfAbsent(ip, k -> apiBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            res.setStatus(429);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"error\":\"Demasiadas solicitudes. Intentá de nuevo en un momento.\"}");
        }
    }

    private String resolveIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    // Auth: 15 requests per minute per IP
    private Bucket authBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(15)
                        .refillGreedy(15, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    // API general: 300 requests per minute per IP
    private Bucket apiBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(300)
                        .refillGreedy(300, Duration.ofMinutes(1))
                        .build())
                .build();
    }
}
