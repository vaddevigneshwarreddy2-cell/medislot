package com.medislot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "hospital_staff")
public class HospitalStaff extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @Column(name = "staff_role", nullable = false, length = 30)
    private String staffRole;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }
    public String getStaffRole() { return staffRole; }
    public void setStaffRole(String staffRole) { this.staffRole = staffRole; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}