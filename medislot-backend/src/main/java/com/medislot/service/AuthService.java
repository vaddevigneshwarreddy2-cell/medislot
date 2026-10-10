package com.medislot.service;

import com.medislot.dto.*;

public interface AuthService {
    RegisterResponse register(RegisterRequest request, String ip);
    void verify(VerifyRequest request, String ip);
    void resendCode(ResendCodeRequest request);
    AuthResponse login(LoginRequest request, String ip);
    AuthResponse refresh(RefreshRequest request);
    void logout(RefreshRequest request);
    UserResponse me(Long userId);
}