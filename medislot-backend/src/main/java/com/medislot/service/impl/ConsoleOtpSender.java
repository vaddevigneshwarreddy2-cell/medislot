package com.medislot.service.impl;

import com.medislot.entity.User;
import com.medislot.service.OtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * DEV ONLY: prints the OTP to the backend console. Disabled under the "prod" profile,
 * when Twilio is enabled, and when verification is switched off.
 */
@Service
@Profile("!prod")
@ConditionalOnExpression("'${SMS_PROVIDER:}' != 'twilio' and '${REQUIRE_VERIFICATION:true}' != 'false'")
public class ConsoleOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleOtpSender.class);

    @Override
    public void send(User user, String code) {
        log.warn("=== DEV OTP === email={} mobile={} code={}", user.getEmail(), user.getMobile(), code);
    }
}