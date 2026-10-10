package com.medislot.repository;

import com.medislot.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

    List<PrescriptionItem> findByPrescriptionIdOrderBySortOrderAsc(Long prescriptionId);

    /** Bulk delete. Flushes first, and deliberately does not clear the persistence context. */
    @Modifying(flushAutomatically = true)
    @Query("delete from PrescriptionItem i where i.prescriptionId = :prescriptionId")
    int deleteAllForPrescription(@Param("prescriptionId") Long prescriptionId);

    @Query("select i.prescriptionId, count(i) from PrescriptionItem i where i.prescriptionId in :ids group by i.prescriptionId")
    List<Object[]> countByPrescription(@Param("ids") Collection<Long> ids);
}