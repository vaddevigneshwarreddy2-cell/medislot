package com.medislot.config;

import com.medislot.entity.*;
import com.medislot.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DEMONSTRATION DATA ONLY. Creates fictional demo accounts (password "Demo@1234") for the college demo.
 * Skips accounts that already exist. Disabled under the "prod" profile or with DEMO_DATA_ENABLED=false.
 */
@Component
@Profile("!prod")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "Demo@1234";

    private record DemoDoctor(String email, String name, String hospital, String dept, String spec,
                              String qualification, int years, int minutes, int[] days, String[] windows) {}

    private static final int[] MON_FRI = {1, 2, 3, 4, 5};
    private static final int[] MON_SAT = {1, 2, 3, 4, 5, 6};
    private static final int[] TUE_SAT = {2, 3, 4, 5, 6};

    private static final List<DemoDoctor> DOCTORS = List.of(
        new DemoDoctor("doctor@demo.com", "Dr. Ravi Kumar", "MediCare Hospital", "Cardiology", "Cardiologist",
                "MD, DM (Cardiology)", 15, 30, MON_FRI, new String[]{"10:00-13:00"}),
        new DemoDoctor("dr.meera@demo.com", "Dr. Meera Nair", "MediCare Hospital", "ENT", "ENT Specialist",
                "MS (ENT)", 9, 20, MON_SAT, new String[]{"09:00-12:00", "15:00-18:00"}),
        new DemoDoctor("dr.arjun@demo.com", "Dr. Arjun Rao", "MediCare Hospital", "General Medicine", "General Physician",
                "MBBS, MD", 12, 15, MON_SAT, new String[]{"09:00-13:00"}),
        new DemoDoctor("dr.sana@demo.com", "Dr. Sana Iqbal", "City Health Hospital", "Pediatrics", "Pediatrician",
                "MD (Pediatrics)", 8, 20, MON_FRI, new String[]{"10:00-14:00"}),
        new DemoDoctor("dr.vikram@demo.com", "Dr. Vikram Reddy", "Lakeview Multispeciality Hospital", "Neurology", "Neurologist",
                "MD, DM (Neurology)", 18, 30, TUE_SAT, new String[]{"11:00-15:00"}),
        new DemoDoctor("dr.latha@demo.com", "Dr. Latha Menon", "Jubilee Hills Heart Institute", "Cardiology", "Cardiologist",
                "MD, DM (Cardiology)", 20, 30, MON_FRI, new String[]{"10:00-13:00"}),
        new DemoDoctor("dr.imran@demo.com", "Dr. Imran Shaikh", "Gachibowli Care Centre", "General Medicine", "General Physician",
                "MBBS", 6, 15, MON_SAT, new String[]{"09:00-13:00"}),
        new DemoDoctor("dr.priya@demo.com", "Dr. Priya Sharma", "Kukatpally Community Hospital", "Gynecology", "Gynecologist",
                "MS (OBG)", 11, 30, MON_FRI, new String[]{"10:00-13:00", "16:00-18:00"})
    );

    private final UserRepository users;
    private final HospitalRepository hospitals;
    private final DepartmentRepository departments;
    private final DoctorRepository doctors;
    private final DoctorAvailabilityRepository availability;
    private final HospitalStaffRepository staff;
    private final PasswordEncoder encoder;
    private final TransactionTemplate tx;
    private final boolean enabled;

    public DemoDataSeeder(UserRepository users, HospitalRepository hospitals, DepartmentRepository departments,
                          DoctorRepository doctors, DoctorAvailabilityRepository availability,
                          HospitalStaffRepository staff, PasswordEncoder encoder,
                          PlatformTransactionManager txManager,
                          @Value("${DEMO_DATA_ENABLED:true}") boolean enabled) {
        this.users = users;
        this.hospitals = hospitals;
        this.departments = departments;
        this.doctors = doctors;
        this.availability = availability;
        this.staff = staff;
        this.encoder = encoder;
        this.tx = new TransactionTemplate(txManager);
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled) return;
        tx.executeWithoutResult(status -> seed());
    }

    private void seed() {
        Map<String, Hospital> byName = hospitals.findAll().stream()
                .collect(Collectors.toMap(Hospital::getName, h -> h, (a, b) -> a));
        Hospital medicare = byName.get("MediCare Hospital");
        if (medicare == null) {
            log.warn("Demo hospitals not found; skipping demo accounts.");
            return;
        }

        int created = 0;
        for (DemoDoctor dd : DOCTORS) {
            Hospital h = byName.get(dd.hospital());
            if (h == null || users.existsByEmail(dd.email())) continue;
            Department dept = departments.findByHospitalIdOrderByNameAsc(h.getId()).stream()
                    .filter(d -> d.getName().equals(dd.dept())).findFirst().orElse(null);
            if (dept == null) continue;

            User u = newUser(dd.email(), dd.name(), Role.DOCTOR);
            Doctor d = new Doctor();
            d.setUserId(u.getId());
            d.setHospitalId(h.getId());
            d.setDepartmentId(dept.getId());
            d.setSpecialization(dd.spec());
            d.setQualification(dd.qualification());
            d.setExperienceYears(dd.years());
            d.setConsultationDurationMin(dd.minutes());
            d.setBio("Demonstration profile (fictional).");
            d = doctors.save(d);

            for (int day : dd.days()) {
                for (String w : dd.windows()) {
                    String[] p = w.split("-");
                    DoctorAvailability a = new DoctorAvailability();
                    a.setDoctorId(d.getId());
                    a.setDayOfWeek(day);
                    a.setStartTime(LocalTime.parse(p[0]));
                    a.setEndTime(LocalTime.parse(p[1]));
                    availability.save(a);
                }
            }
            created++;
        }

        createStaff("admin@demo.com", "Demo Hospital Admin", Role.HOSPITAL_ADMIN, medicare);
        createStaff("reception@demo.com", "Demo Receptionist", Role.RECEPTIONIST, medicare);

        if (created > 0) {
            log.warn("=== DEMO DATA === seeded {} demo doctors plus demo admin/reception accounts "
                   + "(password '{}'). Disable with DEMO_DATA_ENABLED=false.", created, DEMO_PASSWORD);
        }
    }

    private void createStaff(String email, String name, Role role, Hospital hospital) {
        if (users.existsByEmail(email)) return;
        User u = newUser(email, name, role);
        HospitalStaff s = new HospitalStaff();
        s.setUserId(u.getId());
        s.setHospitalId(hospital.getId());
        s.setStaffRole(role.name());
        staff.save(s);
    }

    private User newUser(String email, String name, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setPasswordHash(encoder.encode(DEMO_PASSWORD));
        u.setRole(role);
        u.setVerified(true);
        return users.save(u);
    }
}