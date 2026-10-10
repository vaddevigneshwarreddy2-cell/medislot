package com.medislot.service;

import com.medislot.entity.User;

/** Delivery channel for verification codes. Replaced by the notification system in Phase 8. */
public interface OtpSender {
    void send(User user, String code);
}