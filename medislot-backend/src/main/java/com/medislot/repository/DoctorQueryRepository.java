package com.medislot.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Dynamic doctor search. Only the filters that are present are added, so no null-parameter tricks. */
@Repository
public class DoctorQueryRepository {

    /** Any null field is ignored. specialization and name are lower-case LIKE patterns ('!' escape). */
    public record Filter(Long doctorId, Long hospitalId, Collection<Long> hospitalIds, Long departmentId,
                         String specialization, String name, boolean activeOnly) {}

    private static final String BASE =
            "select new com.medislot.repository.DoctorRow(d.id, u.fullName, u.email, d.specialization, "
          + "d.qualification, d.experienceYears, d.consultationDurationMin, d.bio, d.active, "
          + "h.id, h.name, h.city, dep.id, dep.name) "
          + "from Doctor d, User u, Hospital h, Department dep "
          + "where u.id = d.userId and h.id = d.hospitalId and dep.id = d.departmentId";

    @PersistenceContext
    private EntityManager em;

    public List<DoctorRow> search(Filter f, int limit) {
        StringBuilder q = new StringBuilder(BASE);
        Map<String, Object> p = new LinkedHashMap<>();
        if (f.activeOnly()) {
            q.append(" and d.active = true and u.enabled = true and h.active = true and dep.active = true");
        }
        if (f.doctorId() != null)       { q.append(" and d.id = :doctorId");               p.put("doctorId", f.doctorId()); }
        if (f.hospitalId() != null)     { q.append(" and d.hospitalId = :hospitalId");     p.put("hospitalId", f.hospitalId()); }
        if (f.hospitalIds() != null)    { q.append(" and d.hospitalId in :hospitalIds");   p.put("hospitalIds", f.hospitalIds()); }
        if (f.departmentId() != null)   { q.append(" and d.departmentId = :departmentId"); p.put("departmentId", f.departmentId()); }
        if (f.specialization() != null) { q.append(" and lower(d.specialization) like :spec escape '!'"); p.put("spec", f.specialization()); }
        if (f.name() != null)           { q.append(" and lower(u.fullName) like :name escape '!'");       p.put("name", f.name()); }
        q.append(" order by u.fullName asc, d.id asc");

        TypedQuery<DoctorRow> tq = em.createQuery(q.toString(), DoctorRow.class);
        p.forEach(tq::setParameter);
        tq.setMaxResults(limit);
        return tq.getResultList();
    }

    public Optional<DoctorRow> findById(Long id, boolean activeOnly) {
        return search(new Filter(id, null, null, null, null, null, activeOnly), 1).stream().findFirst();
    }
}