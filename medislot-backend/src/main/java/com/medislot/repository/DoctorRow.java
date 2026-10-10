package com.medislot.repository;

/** One doctor joined with user, hospital and department. Used for search and admin lists. */
public record DoctorRow(Long id, String fullName, String email, String specialization, String qualification,
                        Integer experienceYears, Integer consultationDurationMin, String bio, Boolean active,
                        Long hospitalId, String hospitalName, String hospitalCity,
                        Long departmentId, String departmentName) {}