package com.medislot.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read queries over finalized consultations. Only finalized records are ever returned. */
@Repository
public class ClinicalQueryRepository {

    /** Any null field is ignored. */
    public record Filter(Long prescriptionId, Long patientId, Long doctorId) {}

    private static final String BASE =
            "select new com.medislot.repository.PrescriptionRow(p.id, c.appointmentId, a.appointmentDate, "
          + "du.fullName, d.specialization, d.qualification, h.name, h.addressLine, h.city, pu.fullName, "
          + "c.diagnosis, c.instructions, c.followUpDate, c.doctorId, c.patientId) "
          + "from Prescription p, Consultation c, Appointment a, Doctor d, User du, Hospital h, Patient pt, User pu "
          + "where c.id = p.consultationId and c.finalized = true and a.id = c.appointmentId "
          + "and d.id = c.doctorId and du.id = d.userId and h.id = c.hospitalId "
          + "and pt.id = c.patientId and pu.id = pt.userId";

    @PersistenceContext
    private EntityManager em;

    public List<PrescriptionRow> search(Filter f, int limit) {
        StringBuilder q = new StringBuilder(BASE);
        Map<String, Object> p = new LinkedHashMap<>();
        if (f.prescriptionId() != null) { q.append(" and p.id = :rx");       p.put("rx", f.prescriptionId()); }
        if (f.patientId() != null)      { q.append(" and c.patientId = :pt"); p.put("pt", f.patientId()); }
        if (f.doctorId() != null)       { q.append(" and c.doctorId = :dr");  p.put("dr", f.doctorId()); }
        q.append(" order by a.appointmentDate desc, p.id desc");
        TypedQuery<PrescriptionRow> tq = em.createQuery(q.toString(), PrescriptionRow.class);
        p.forEach(tq::setParameter);
        tq.setMaxResults(limit);
        return tq.getResultList();
    }

    public List<FollowUpRow> followUps(Long patientId, LocalDate from) {
        return em.createQuery("select new com.medislot.repository.FollowUpRow(c.appointmentId, c.followUpDate, "
                        + "d.id, du.fullName, d.specialization, h.name) "
                        + "from Consultation c, Doctor d, User du, Hospital h "
                        + "where d.id = c.doctorId and du.id = d.userId and h.id = c.hospitalId "
                        + "and c.patientId = :pt and c.finalized = true and c.followUpDate >= :from "
                        + "order by c.followUpDate asc", FollowUpRow.class)
                .setParameter("pt", patientId).setParameter("from", from)
                .setMaxResults(50).getResultList();
    }
}