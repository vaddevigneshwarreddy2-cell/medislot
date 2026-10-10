package com.medislot.service.channel;

import com.medislot.entity.NotificationType;
import com.medislot.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/** DEV ONLY, off unless SMS_PROVIDER=mock. Only time-sensitive messages are texted; the number is masked. */
@Component
@Profile("!prod")
@ConditionalOnProperty(name = "SMS_PROVIDER", havingValue = "mock")
public class MockSmsChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(MockSmsChannel.class);
    private static final Set<NotificationType> URGENT = EnumSet.of(NotificationType.ATTENDANCE_REQUEST,
            NotificationType.APPOINTMENT_REMINDER, NotificationType.DOCTOR_LEAVE, NotificationType.APPOINTMENT_CANCELLED);

    @Override
    public void deliver(User user, NotificationType type, String title, String message) {
        String mobile = user.getMobile();
        if (mobile == null || mobile.length() < 4 || !URGENT.contains(type)) return;
        log.info("[MOCK SMS] to=***{} type={}", mobile.substring(mobile.length() - 4), type);
    }
}