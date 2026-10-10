package com.medislot.repository;

import com.medislot.entity.Appointment;
import com.medislot.entity.AppointmentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Extra read queries for notifications, reminders and dashboards. */
public interface AppointmentInsightRepository extends Repository<Appointment, Long> {

    long countByPatientIdAndStatus(Long patientId, AppointmentStatus status);

    Optional<Appointment> findFirstByRescheduledFromId(Long rescheduledFromId);

    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusIn(Long doctorId, LocalDate date,
                                                                  Collection<AppointmentStatus> statuses);

    @Query("select a.id from Appointment a where a.status = :status and a.attendanceRequestedAt is null "
         + "and a.slotStart > :now and a.slotStart <= :until order by a.slotStart")
    List<Long> attendanceCandidates(@Param("status") AppointmentStatus status, @Param("now") Instant now,
                                    @Param("until") Instant until, Pageable pageable);

    @Query("select a.id from Appointment a where a.status in :statuses and a.reminderSentAt is null "
         + "and a.slotStart > :now and a.slotStart <= :until order by a.slotStart")
    List<Long> reminderCandidates(@Param("statuses") Collection<AppointmentStatus> statuses,
                                  @Param("now") Instant now, @Param("until") Instant until, Pageable pageable);

    @Query("select count(a) from Appointment a where a.doctorId = :doctorId and a.appointmentDate > :date "
         + "and a.status in :statuses")
    long countFuture(@Param("doctorId") Long doctorId, @Param("date") LocalDate date,
                     @Param("statuses") Collection<AppointmentStatus> statuses);
}