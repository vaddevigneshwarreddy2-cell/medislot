package com.medislot.controller;

import com.medislot.dto.*;
import com.medislot.security.AuthUser;
import com.medislot.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletRequest http) {
        return ApiResponse.ok("Registration successful. Enter the verification code we sent you.",
                authService.register(request, http.getRemoteAddr()));
    }

    @PostMapping("/verify")
    public ApiResponse<Void> verify(@Valid @RequestBody VerifyRequest request, HttpServletRequest http) {
        authService.verify(request, http.getRemoteAddr());
        return ApiResponse.ok("Account verified. You can now log in.", null);
    }

    @PostMapping("/resend-code")
    public ApiResponse<Void> resend(@Valid @RequestBody ResendCodeRequest request) {
        authService.resendCode(request);
        return ApiResponse.ok("If the account exists and is unverified, a new code has been sent.", null);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Login successful.", authService.login(request, http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok("Token refreshed.", authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ApiResponse.ok("Logged out.", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthUser principal) {
        return ApiResponse.ok("OK", authService.me(principal.id()));
    }
}