package com.medislot.service.impl;

import com.medislot.entity.User;
import com.medislot.service.OtpSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Used when REQUIRE_VERIFICATION=false. Nothing is sent. */
@Service
@ConditionalOnProperty(name = "REQUIRE_VERIFICATION", havingValue = "false")
public class NoopOtpSender implements OtpSender {
    @Override
    public void send(User user, String code) { }
}