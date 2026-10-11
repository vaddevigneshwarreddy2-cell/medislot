package com.medislot.repository;

public record StaffMemberRow(Long id, String fullName, String email, String mobile, String staffRole,
                             Boolean active, Boolean enabled) {}