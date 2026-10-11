package com.medislot.dto;

import jakarta.validation.constraints.*;

public final class AccountDtos {

    private AccountDtos() {}

    public record CreateAccountRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 255) String email,
            @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "must be 10-15 digits") String mobile,
            @NotBlank @Size(min = 8, max = 72)
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                     message = "must contain an uppercase letter, a lowercase letter and a digit") String password) {}

    public record StaffMember(Long id, String fullName, String email, String mobile, String role, boolean active) {}

    public record HospitalRow(Long id, String name, String city, String phone, String email, boolean active) {}
}