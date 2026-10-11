package com.medislot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medislot.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Per-IP request limits. Runs just after the Spring Security chain (order -100) so a 429 still carries the
 * CORS headers the browser needs. The client address is the connection address, never a forwarded header.
 */
@Component
@Order(-99)
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String name, int limit, long windowMs) {}

    private static final long MINUTE = 60_000L;
    private static final long TEN_MINUTES = 600_000L;

    private final boolean enabled;
    private final Rule login;
    private final Rule register;
    private final Rule resend;
    private final Rule verify;
    private final Rule refresh;
    private final Rule general;
    private final RateLimiter limiter = new RateLimiter();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public RateLimitFilter(@Value("${RATE_LIMIT_ENABLED:true}") boolean enabled,
                           @Value("${RATE_LIMIT_LOGIN_PER_MIN:10}") int loginPerMin,
                           @Value("${RATE_LIMIT_REGISTER_PER_10MIN:5}") int registerPer10Min,
                           @Value("${RATE_LIMIT_RESEND_PER_10MIN:3}") int resendPer10Min,
                           @Value("${RATE_LIMIT_VERIFY_PER_10MIN:10}") int verifyPer10Min,
                           @Value("${RATE_LIMIT_REFRESH_PER_MIN:30}") int refreshPerMin,
                           @Value("${RATE_LIMIT_GENERAL_PER_MIN:300}") int generalPerMin) {
        this.enabled = enabled;
        this.login = new Rule("login", loginPerMin, MINUTE);
        this.register = new Rule("register", registerPer10Min, TEN_MINUTES);
        this.resend = new Rule("resend", resendPer10Min, TEN_MINUTES);
        this.verify = new Rule("verify", verifyPer10Min, TEN_MINUTES);
        this.refresh = new Rule("refresh", refreshPerMin, MINUTE);
        this.general = new Rule("general", generalPerMin, MINUTE);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !enabled || path == null || !path.startsWith("/api/") || path.equals("/api/health")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = ruleFor(request);
        RateLimiter.Result result = limiter.tryAcquire(rule.name() + ":" + request.getRemoteAddr(),
                rule.limit(), rule.windowMs(), System.currentTimeMillis());
        if (result.allowed()) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        response.setContentType("application/json");
        mapper.writeValue(response.getWriter(),
                ApiResponse.error("RATE_LIMITED", "Too many requests. Please wait a moment and try again."));
    }

    private Rule ruleFor(HttpServletRequest request) {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String path = request.getRequestURI();
            if (path.equals("/api/auth/login")) return login;
            if (path.equals("/api/auth/register")) return register;
            if (path.equals("/api/auth/resend-code")) return resend;
            if (path.equals("/api/auth/verify")) return verify;
            if (path.equals("/api/auth/refresh")) return refresh;
        }
        return general;
    }
}