package com.medislot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Typed access to the "medislot.*" settings so values are never hard-coded. */
@ConfigurationProperties(prefix = "medislot")
public record AppProperties(Search search, Booking booking, Cors cors, Jwt jwt, Auth auth) {

    public record Search(int defaultRadiusKm, List<Integer> radiusOptionsKm) {}

    public record Booking(int maxActiveAppointments) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Jwt(String secret, long accessTokenMinutes, long refreshTokenDays) {}

    public record Auth(int otpExpiryMinutes, int otpMaxAttempts, int otpResendCooldownSeconds,
                       int maxFailedLogins, int lockMinutes) {}
}