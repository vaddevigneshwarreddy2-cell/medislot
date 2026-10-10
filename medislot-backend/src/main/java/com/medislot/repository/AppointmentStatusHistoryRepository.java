package com.medislot.repository;

import com.medislot.entity.AppointmentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentStatusHistoryRepository extends JpaRepository<AppointmentStatusHistory, Long> {
}