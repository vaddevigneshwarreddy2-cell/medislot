package com.medislot.config;

import com.medislot.entity.Role;
import com.medislot.entity.User;
import com.medislot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Creates the first platform administrator from BOOTSTRAP_SUPERADMIN_EMAIL / BOOTSTRAP_SUPERADMIN_PASSWORD.
 * Active only when the email is set. Does nothing if that account already exists.
 * Remove both variables after the first login. The password is never logged.
 */
@Component
@ConditionalOnExpression("!'${BOOTSTRAP_SUPERADMIN_EMAIL:}'.isEmpty()")
public class BootstrapSuperAdmin implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapSuperAdmin.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;
    private final String name;

    public BootstrapSuperAdmin(UserRepository users, PasswordEncoder encoder,
                               @Value("${BOOTSTRAP_SUPERADMIN_EMAIL:}") String email,
                               @Value("${BOOTSTRAP_SUPERADMIN_PASSWORD:}") String password,
                               @Value("${BOOTSTRAP_SUPERADMIN_NAME:Platform Administrator}") String name) {
        this.users = users;
        this.encoder = encoder;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(String... args) {
        if (!email.matches("^\\S+@\\S+\\.\\S+$")) {
            throw new IllegalStateException("BOOTSTRAP_SUPERADMIN_EMAIL is not a valid email address.");
        }
        if (password.length() < 12 || !password.matches(".*[a-z].*") || !password.matches(".*[A-Z].*")
                || !password.matches(".*\\d.*")) {
            throw new IllegalStateException("BOOTSTRAP_SUPERADMIN_PASSWORD must be at least 12 characters "
                    + "with upper case, lower case and a digit.");
        }
        if (users.existsByEmail(email)) return;
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setPasswordHash(encoder.encode(password));
        u.setRole(Role.SUPER_ADMIN);
        u.setVerified(true);
        users.save(u);
        log.warn("Created the platform administrator {}. Remove the BOOTSTRAP_SUPERADMIN_* settings now.", email);
    }
}