package com.medislot.repository;

import com.medislot.entity.AppointmentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Grouped counts for the hospital admin dashboard. Held, expired and replaced rows are not real bookings. */
@Repository
public class DashboardQueryRepository {

    private static final Set<AppointmentStatus> EXCLUDED =
            EnumSet.of(AppointmentStatus.HELD, AppointmentStatus.EXPIRED, AppointmentStatus.RESCHEDULED);

    @PersistenceContext
    private EntityManager em;

    /** Rows of [doctorId, doctorName, status, count]. */
    public List<Object[]> byDoctor(Long hospitalId, LocalDate from, LocalDate to) {
        return em.createQuery("select d.id, du.fullName, a.status, count(a) "
                        + "from Appointment a, Doctor d, User du "
                        + "where d.id = a.doctorId and du.id = d.userId and a.hospitalId = :h "
                        + "and a.appointmentDate between :from and :to and a.status not in :excluded "
                        + "group by d.id, du.fullName, a.status", Object[].class)
                .setParameter("h", hospitalId).setParameter("from", from).setParameter("to", to)
                .setParameter("excluded", EXCLUDED).getResultList();
    }

    /** Rows of [departmentId, departmentName, status, count]. */
    public List<Object[]> byDepartment(Long hospitalId, LocalDate from, LocalDate to) {
        return em.createQuery("select dep.id, dep.name, a.status, count(a) "
                        + "from Appointment a, Department dep "
                        + "where dep.id = a.departmentId and a.hospitalId = :h "
                        + "and a.appointmentDate between :from and :to and a.status not in :excluded "
                        + "group by dep.id, dep.name, a.status", Object[].class)
                .setParameter("h", hospitalId).setParameter("from", from).setParameter("to", to)
                .setParameter("excluded", EXCLUDED).getResultList();
    }

    public long checkedIn(Long hospitalId, LocalDate from, LocalDate to) {
        return ((Number) em.createQuery("select count(a) from Appointment a where a.hospitalId = :h "
                        + "and a.appointmentDate between :from and :to and a.checkedInAt is not null")
                .setParameter("h", hospitalId).setParameter("from", from).setParameter("to", to)
                .getSingleResult()).longValue();
    }
}