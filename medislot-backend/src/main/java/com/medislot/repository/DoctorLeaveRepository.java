package com.medislot.repository;

import com.medislot.entity.DoctorLeave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, Long> {
    boolean existsByDoctorIdAndLeaveDate(Long doctorId, LocalDate leaveDate);
    List<DoctorLeave> findByDoctorIdAndLeaveDateGreaterThanEqualOrderByLeaveDateAsc(Long doctorId, LocalDate from);
}