package com.medislot.repository;

import com.medislot.entity.AppointmentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dynamic appointment listing for staff. Only the filters that are present are added. */
@Repository
public class StaffQueryRepository {

    /** Any null field is ignored. namePattern is a lower-case LIKE pattern with '!' as escape. */
    public record Filter(Long hospitalId, Long doctorId, LocalDate date,
                         Collection<AppointmentStatus> statuses, String namePattern, Long appointmentId) {}

    private static final String BASE =
            "select new com.medislot.repository.StaffRow(a.id, a.status, a.appointmentDate, a.slotStart, a.slotEnd, "
          + "a.queueNumber, a.checkedInAt, a.calledAt, a.source, a.reason, pu.fullName, pu.mobile, "
          + "d.id, du.fullName, d.specialization, d.consultationDurationMin) "
          + "from Appointment a, Patient p, User pu, Doctor d, User du "
          + "where p.id = a.patientId and pu.id = p.userId and d.id = a.doctorId and du.id = d.userId";

    @PersistenceContext
    private EntityManager em;

    public List<StaffRow> search(Filter f, int limit) {
        StringBuilder q = new StringBuilder(BASE);
        Map<String, Object> p = new LinkedHashMap<>();
        if (f.hospitalId() != null)    { q.append(" and a.hospitalId = :hospitalId");           p.put("hospitalId", f.hospitalId()); }
        if (f.doctorId() != null)      { q.append(" and a.doctorId = :doctorId");               p.put("doctorId", f.doctorId()); }
        if (f.date() != null)          { q.append(" and a.appointmentDate = :date");            p.put("date", f.date()); }
        if (f.statuses() != null)      { q.append(" and a.status in :statuses");                p.put("statuses", f.statuses()); }
        if (f.appointmentId() != null) { q.append(" and a.id = :appointmentId");                p.put("appointmentId", f.appointmentId()); }
        if (f.namePattern() != null)   { q.append(" and lower(pu.fullName) like :name escape '!'"); p.put("name", f.namePattern()); }
        q.append(" order by a.slotStart asc, a.queueNumber asc, a.id asc");

        TypedQuery<StaffRow> tq = em.createQuery(q.toString(), StaffRow.class);
        p.forEach(tq::setParameter);
        tq.setMaxResults(limit);
        return tq.getResultList();
    }

    public int maxQueueNumber(Long doctorId, LocalDate date) {
        Object r = em.createQuery("select coalesce(max(a.queueNumber), 0) from Appointment a "
                        + "where a.doctorId = :d and a.appointmentDate = :date")
                .setParameter("d", doctorId).setParameter("date", date).getSingleResult();
        return ((Number) r).intValue();
    }
}