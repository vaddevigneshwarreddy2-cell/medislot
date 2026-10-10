package com.medislot.event;

import java.time.LocalDate;

public record LeaveCreated(Long doctorId, LocalDate date) {}