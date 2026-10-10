package com.medislot.mapper;

import com.medislot.dto.HospitalDtos.DepartmentInfo;
import com.medislot.dto.HospitalDtos.Detail;
import com.medislot.dto.HospitalDtos.Summary;
import com.medislot.entity.Department;
import com.medislot.entity.Hospital;
import com.medislot.repository.DepartmentRepository;
import com.medislot.repository.HospitalRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds hospital responses and adds doctor counts and departments with batch queries. */
@Component
public class HospitalMapper {

    /** Basic hospital fields plus an optional distance, before enrichment. */
    public record Core(Long id, String name, String addressLine, String city, String state,
                       BigDecimal latitude, BigDecimal longitude, String phone, Double distanceKm) {}

    private final HospitalRepository hospitals;
    private final DepartmentRepository departments;

    public HospitalMapper(HospitalRepository hospitals, DepartmentRepository departments) {
        this.hospitals = hospitals;
        this.departments = departments;
    }

    public static Core core(Hospital h) {
        return new Core(h.getId(), h.getName(), h.getAddressLine(), h.getCity(), h.getState(),
                h.getLatitude(), h.getLongitude(), h.getPhone(), null);
    }

    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public List<Summary> toSummaries(List<Core> cores) {
        if (cores.isEmpty()) return List.of();
        List<Long> ids = cores.stream().map(Core::id).toList();

        Map<Long, Long> counts = new HashMap<>();
        hospitals.countActiveDoctors(ids).forEach(r -> counts.put(r.getHospitalId(), r.getTotal()));

        Map<Long, List<String>> depts = departments.findByHospitalIdInAndActiveTrueOrderByNameAsc(ids).stream()
                .collect(Collectors.groupingBy(Department::getHospitalId,
                        Collectors.mapping(Department::getName, Collectors.toList())));

        return cores.stream().map(c -> new Summary(
                c.id(), c.name(), c.addressLine(), c.city(), c.state(), c.latitude(), c.longitude(), c.phone(),
                c.distanceKm() == null ? null : round1(c.distanceKm()),
                counts.getOrDefault(c.id(), 0L),
                depts.getOrDefault(c.id(), List.of()))).toList();
    }

    public Detail toDetail(Hospital h, Double distanceKm) {
        long doctors = hospitals.countActiveDoctors(List.of(h.getId())).stream()
                .mapToLong(HospitalRepository.DoctorCountRow::getTotal).sum();
        List<DepartmentInfo> deps = departments.findByHospitalIdAndActiveTrueOrderByNameAsc(h.getId()).stream()
                .map(d -> new DepartmentInfo(d.getId(), d.getName(), d.getDescription())).toList();
        return new Detail(h.getId(), h.getName(), h.getAddressLine(), h.getCity(), h.getState(),
                h.getPostalCode(), h.getCountry(), h.getLatitude(), h.getLongitude(), h.getPhone(),
                h.getEmail(), h.isActive(), distanceKm == null ? null : round1(distanceKm), doctors, deps);
    }
}