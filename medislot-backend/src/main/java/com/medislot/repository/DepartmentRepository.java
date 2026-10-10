package com.medislot.repository;

import com.medislot.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByHospitalIdInAndActiveTrueOrderByNameAsc(Collection<Long> hospitalIds);
    List<Department> findByHospitalIdAndActiveTrueOrderByNameAsc(Long hospitalId);
    List<Department> findByHospitalIdOrderByNameAsc(Long hospitalId);
    boolean existsByHospitalIdAndNameIgnoreCase(Long hospitalId, String name);
}