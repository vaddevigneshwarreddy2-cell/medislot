package com.medislot.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StaffMemberQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<StaffMemberRow> find(Long hospitalId, String staffRole) {
        return em.createQuery("select new com.medislot.repository.StaffMemberRow(s.id, u.fullName, u.email, "
                        + "u.mobile, s.staffRole, s.active, u.enabled) "
                        + "from HospitalStaff s, User u where u.id = s.userId and s.hospitalId = :h "
                        + "and s.staffRole = :r order by u.fullName", StaffMemberRow.class)
                .setParameter("h", hospitalId).setParameter("r", staffRole).setMaxResults(500).getResultList();
    }
}