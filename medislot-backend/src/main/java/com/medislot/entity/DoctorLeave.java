package com.medislot.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "doctor_leave")
public class DoctorLeave extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "leave_date", nullable = false)
    private LocalDate leaveDate;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_by")
    private Long createdBy;

    public Long getId() { return id; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public LocalDate getLeaveDate() { return leaveDate; }
    public void setLeaveDate(LocalDate leaveDate) { this.leaveDate = leaveDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}