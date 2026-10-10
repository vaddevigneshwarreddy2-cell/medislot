package com.medislot.security;

import com.medislot.entity.Role;

/** The authenticated principal rebuilt from a valid JWT on each request. */
public record AuthUser(Long id, String email, Role role) {}