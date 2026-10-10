package com.medislot.repository;

import com.medislot.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    Optional<Consultation> findByAppointmentId(Long appointmentId);

    @Query("select c from Consultation c where c.finalized = true and c.followUpDate between :from and :to "
         + "order by c.followUpDate")
    List<Consultation> findFollowUpsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}