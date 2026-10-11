package com.medislot.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    // enabled, login/min, register/10min, resend/10min, verify/10min, refresh/min, general/min
    private final RateLimitFilter filter = new RateLimitFilter(true, 2, 5, 3, 10, 30, 300);

    private MockHttpServletResponse call(String method, String path, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    void thirdLoginAttemptFromOneIpIsBlocked() throws Exception {
        assertEquals(200, call("POST", "/api/auth/login", "1.1.1.1").getStatus());
        assertEquals(200, call("POST", "/api/auth/login", "1.1.1.1").getStatus());
        MockHttpServletResponse blocked = call("POST", "/api/auth/login", "1.1.1.1");
        assertEquals(429, blocked.getStatus());
        assertNotNull(blocked.getHeader("Retry-After"));
        assertTrue(blocked.getContentAsString().contains("RATE_LIMITED"));
    }

    @Test
    void anotherIpIsNotAffected() throws Exception {
        for (int i = 0; i < 3; i++) call("POST", "/api/auth/login", "2.2.2.2");
        assertEquals(200, call("POST", "/api/auth/login", "3.3.3.3").getStatus());
    }

    @Test
    void healthAndPreflightAndNonApiPathsAreNeverLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, call("GET", "/api/health", "4.4.4.4").getStatus());
            assertEquals(200, call("OPTIONS", "/api/auth/login", "4.4.4.4").getStatus());
            assertEquals(200, call("GET", "/index.html", "4.4.4.4").getStatus());
        }
    }

    @Test
    void disabledFilterLetsEverythingThrough() throws Exception {
        RateLimitFilter off = new RateLimitFilter(false, 1, 1, 1, 1, 1, 1);
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            MockHttpServletResponse res = new MockHttpServletResponse();
            off.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }
    }
}