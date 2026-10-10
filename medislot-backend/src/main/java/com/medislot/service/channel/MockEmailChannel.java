package com.medislot.service.channel;

import com.medislot.entity.NotificationType;
import com.medislot.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** DEV ONLY: "sends" email by logging the recipient and subject. The message body is never logged. */
@Component
@Profile("!prod")
public class MockEmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(MockEmailChannel.class);

    @Override
    public void deliver(User user, NotificationType type, String title, String message) {
        if (user.getEmail() == null || user.getEmail().endsWith(WALK_IN_DOMAIN)) return;
        log.info("[MOCK EMAIL] to={} type={} subject=\"{}\"", user.getEmail(), type, title);
    }
}