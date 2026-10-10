package com.medislot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);

    private final ReminderService reminders;
    private final boolean enabled;

    public ReminderJob(ReminderService reminders, @Value("${REMINDERS_ENABLED:true}") boolean enabled) {
        this.reminders = reminders;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${REMINDER_INTERVAL_MS:300000}", initialDelayString = "${REMINDER_INITIAL_DELAY_MS:60000}")
    public void run() {
        if (!enabled) return;
        try {
            reminders.runOnce();
        } catch (Exception e) {
            log.warn("Reminder run failed: {}", e.getMessage());
        }
    }
}