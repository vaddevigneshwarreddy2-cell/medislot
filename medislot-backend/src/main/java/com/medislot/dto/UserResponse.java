package com.medislot.dto;

import com.medislot.entity.Role;

public record UserResponse(Long id, String email, String fullName, String mobile, Role role, boolean verified) {}