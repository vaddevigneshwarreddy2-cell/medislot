package com.medislot.dto;

public record AuthResponse(String accessToken, String refreshToken, String tokenType,
                           long expiresInSeconds, UserResponse user) {}