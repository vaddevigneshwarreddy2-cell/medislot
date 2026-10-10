package com.medislot.repository;

import com.medislot.entity.HospitalStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HospitalStaffRepository extends JpaRepository<HospitalStaff, Long> {
    Optional<HospitalStaff> findByUserIdAndActiveTrue(Long userId);
}