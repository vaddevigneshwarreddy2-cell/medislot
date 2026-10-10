package com.medislot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/** All hospital-related request/response shapes. Entities are never exposed directly. */
public final class HospitalDtos {

    private HospitalDtos() {}

    public record Summary(Long id, String name, String addressLine, String city, String state,
                          BigDecimal latitude, BigDecimal longitude, String phone,
                          Double distanceKm, long availableDoctors, List<String> departments) {}

    public record DepartmentInfo(Long id, String name, String description) {}

    public record Detail(Long id, String name, String addressLine, String city, String state,
                         String postalCode, String country, BigDecimal latitude, BigDecimal longitude,
                         String phone, String email, boolean active, Double distanceKm,
                         long availableDoctors, List<DepartmentInfo> departments) {}

    public record NearbyResponse(Double radiusKm, int count, List<Summary> hospitals) {}

    public record SearchConfig(int defaultRadiusKm, List<Integer> radiusOptionsKm) {}

    public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {}

    public record ProfileRequest(
            @NotBlank @Size(max = 200) String name,
            @Pattern(regexp = "^\\+?[0-9 \\-]{7,20}$", message = "must be a valid phone number") String phone,
            @Email @Size(max = 255) String email
    ) {}

    public record LocationRequest(
            @NotBlank @Size(max = 255) String addressLine,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @Size(max = 20) String postalCode,
            @Size(max = 100) String country,
            @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
            @Digits(integer = 3, fraction = 7) BigDecimal latitude,
            @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
            @Digits(integer = 3, fraction = 7) BigDecimal longitude
    ) {}

    public record CreateRequest(@Valid @NotNull ProfileRequest profile,
                                @Valid @NotNull LocationRequest location) {}

    public record ActiveRequest(@NotNull Boolean active) {}
}