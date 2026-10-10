package com.medislot.repository;

import com.medislot.entity.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {

    List<DoctorAvailability> findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(Long doctorId);

    List<DoctorAvailability> findByDoctorIdAndDayOfWeekOrderByStartTimeAsc(Long doctorId, Integer dayOfWeek);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DoctorAvailability a where a.doctorId = :doctorId")
    int deleteAllForDoctor(@Param("doctorId") Long doctorId);
}