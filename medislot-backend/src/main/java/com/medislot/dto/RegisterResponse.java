package com.medislot.dto;

public record RegisterResponse(String email, boolean verificationRequired) {}