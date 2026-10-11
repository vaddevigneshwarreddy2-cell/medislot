package com.medislot.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Native aggregate queries for reports. Held, expired and replaced rows are not real bookings. */
@Repository
public class ReportQueryRepository {

    private static final Set<String> UNITS = Set.of("day", "week", "month");

    @PersistenceContext
    private EntityManager em;

    /** Rows of [bucket start (yyyy-MM-dd), status, count]. unit must be day, week or month. */
    @SuppressWarnings("unchecked")
    public List<Object[]> series(Long hospitalId, LocalDate from, LocalDate to, String unit) {
        if (!UNITS.contains(unit)) throw new IllegalArgumentException("Bad unit");
        String sql = "SELECT to_char(date_trunc('" + unit + "', CAST(a.appointment_date AS timestamp)), 'YYYY-MM-DD'), "
                + "a.status, COUNT(*) FROM appointments a "
                + "WHERE a.hospital_id = :h AND a.appointment_date BETWEEN :from AND :to "
                + "AND a.status NOT IN ('HELD','EXPIRED','RESCHEDULED') "
                + "GROUP BY 1, 2 ORDER BY 1";
        return em.createNativeQuery(sql).setParameter("h", hospitalId).setParameter("from", from)
                .setParameter("to", to).getResultList();
    }

    /** One row of [average minutes from check-in to consultation start (may be null), sample count]. */
    public Object[] averageWait(Long hospitalId, LocalDate from, LocalDate to) {
        String sql = "SELECT AVG(EXTRACT(EPOCH FROM (a.consultation_started_at - a.checked_in_at)) / 60.0), COUNT(*) "
                + "FROM appointments a WHERE a.hospital_id = :h AND a.appointment_date BETWEEN :from AND :to "
                + "AND a.checked_in_at IS NOT NULL AND a.consultation_started_at IS NOT NULL "
                + "AND a.consultation_started_at >= a.checked_in_at";
        return (Object[]) em.createNativeQuery(sql).setParameter("h", hospitalId).setParameter("from", from)
                .setParameter("to", to).getSingleResult();
    }
}