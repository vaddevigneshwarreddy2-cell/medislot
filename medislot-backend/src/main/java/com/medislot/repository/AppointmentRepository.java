package com.medislot.repository;

import com.medislot.entity.Appointment;
import com.medislot.entity.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") Long id);

    /** Expired holds still sitting on a slot. Locked so concurrent bookers do not both clean it up. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.doctorId = :doctorId and a.slotStart = :start "
         + "and a.status = :held and a.holdExpiresAt < :now")
    List<Appointment> findStaleHoldsForSlot(@Param("doctorId") Long doctorId, @Param("start") Instant start,
                                            @Param("held") AppointmentStatus held, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.patientId = :patientId and a.status = :held")
    List<Appointment> findHoldsOfPatient(@Param("patientId") Long patientId,
                                         @Param("held") AppointmentStatus held);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.status = :held and a.holdExpiresAt < :now order by a.holdExpiresAt")
    List<Appointment> findExpiredHolds(@Param("held") AppointmentStatus held, @Param("now") Instant now,
                                       Pageable pageable);

    @Query("select count(a) from Appointment a where a.patientId = :patientId "
         + "and a.status in :active and a.slotEnd > :now")
    long countActive(@Param("patientId") Long patientId, @Param("active") Collection<AppointmentStatus> active,
                     @Param("now") Instant now);

    @Query("select count(a) from Appointment a where a.patientId = :patientId and a.id <> :excludeId "
         + "and a.status in :statuses and a.doctorId = :doctorId and a.appointmentDate = :date")
    long countSameDoctorDay(@Param("patientId") Long patientId, @Param("excludeId") Long excludeId,
                            @Param("statuses") Collection<AppointmentStatus> statuses,
                            @Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    @Query("select count(a) from Appointment a where a.patientId = :patientId and a.id <> :excludeId "
         + "and a.status in :statuses and a.slotStart < :end and a.slotEnd > :start")
    long countOverlapping(@Param("patientId") Long patientId, @Param("excludeId") Long excludeId,
                          @Param("statuses") Collection<AppointmentStatus> statuses,
                          @Param("start") Instant start, @Param("end") Instant end);

    @Query("select a from Appointment a where a.patientId = :patientId and a.status in :active "
         + "and a.slotEnd > :now order by a.slotStart asc")
    List<Appointment> findUpcoming(@Param("patientId") Long patientId,
                                   @Param("active") Collection<AppointmentStatus> active,
                                   @Param("now") Instant now, Pageable pageable);

    @Query("select a from Appointment a where a.patientId = :patientId and a.status not in :hidden "
         + "and (a.status not in :active or a.slotEnd <= :now) order by a.slotStart desc")
    List<Appointment> findHistory(@Param("patientId") Long patientId,
                                  @Param("hidden") Collection<AppointmentStatus> hidden,
                                  @Param("active") Collection<AppointmentStatus> active,
                                  @Param("now") Instant now, Pageable pageable);

    /** Slot start times that are unavailable: booked, or held and not yet expired. */
    @Query("select a.slotStart from Appointment a where a.doctorId = :doctorId and a.slotStart >= :from "
         + "and a.slotStart < :to and (a.status in :occupied or (a.status = :held and a.holdExpiresAt > :now))")
    List<Instant> findTakenStarts(@Param("doctorId") Long doctorId, @Param("from") Instant from,
                                  @Param("to") Instant to,
                                  @Param("occupied") Collection<AppointmentStatus> occupied,
                                  @Param("held") AppointmentStatus held, @Param("now") Instant now);
}