package com.medislot.repository;

import com.medislot.entity.HospitalHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HospitalHolidayRepository extends JpaRepository<HospitalHoliday, Long> {
    boolean existsByHospitalIdAndHolidayDate(Long hospitalId, LocalDate holidayDate);
    List<HospitalHoliday> findByHospitalIdAndHolidayDateGreaterThanEqualOrderByHolidayDateAsc(Long hospitalId, LocalDate from);
}