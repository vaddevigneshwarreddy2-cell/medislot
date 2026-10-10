package com.medislot.service.channel;

import com.medislot.entity.NotificationType;
import com.medislot.entity.User;

/** A delivery channel besides the in-app inbox. Add a real email or SMS provider as one more implementation. */
public interface NotificationChannel {

    /** Accounts created at the front desk have no real email address. */
    String WALK_IN_DOMAIN = "@walkin.medislot.local";

    void deliver(User user, NotificationType type, String title, String message);
}