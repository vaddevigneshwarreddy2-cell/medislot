package com.medislot.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "check_ins")
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false, unique = true)
    private Long appointmentId;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @Column(name = "checked_in_by")
    private Long checkedInBy;

    @Column(nullable = false, length = 20)
    private String method;

    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt = Instant.now();

    public Long getId() { return id; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }
    public void setCheckedInBy(Long checkedInBy) { this.checkedInBy = checkedInBy; }
    public void setMethod(String method) { this.method = method; }
    public void setCheckedInAt(Instant checkedInAt) { this.checkedInAt = checkedInAt; }
}