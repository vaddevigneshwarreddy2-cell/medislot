package com.medislot.config;

import com.medislot.entity.Role;
import com.medislot.entity.User;
import com.medislot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** DEMONSTRATION ONLY: creates super@demo.com (password Demo@1234). Never runs under the "prod" profile. */
@Component
@Profile("!prod")
public class DemoSuperAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSuperAdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final boolean enabled;

    public DemoSuperAdminSeeder(UserRepository users, PasswordEncoder encoder,
                                @Value("${DEMO_DATA_ENABLED:true}") boolean enabled) {
        this.users = users;
        this.encoder = encoder;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled || users.existsByEmail("super@demo.com")) return;
        User u = new User();
        u.setEmail("super@demo.com");
        u.setFullName("Demo Super Admin");
        u.setPasswordHash(encoder.encode("Demo@1234"));
        u.setRole(Role.SUPER_ADMIN);
        u.setVerified(true);
        users.save(u);
        log.warn("=== DEMO DATA === created super@demo.com (password 'Demo@1234'). Disable with DEMO_DATA_ENABLED=false.");
    }
}