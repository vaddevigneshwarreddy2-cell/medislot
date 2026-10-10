package com.medislot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Housekeeping only: an expired hold is already treated as free, this just closes the rows. */
@Component
public class HoldExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(HoldExpiryJob.class);

    private final AppointmentService appointments;

    public HoldExpiryJob(AppointmentService appointments) {
        this.appointments = appointments;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void run() {
        try {
            appointments.expireStaleHolds();
        } catch (Exception e) {
            log.warn("Hold expiry run failed: {}", e.getMessage());
        }
    }
}