package com.medislot.service;

import com.medislot.entity.Appointment;
import com.medislot.entity.Hospital;
import com.medislot.entity.Patient;
import com.medislot.repository.AppointmentRepository;
import com.medislot.repository.DoctorQueryRepository;
import com.medislot.repository.DoctorRow;
import com.medislot.repository.HospitalRepository;
import com.medislot.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Looks up the names and times that notification texts need. Call inside a transaction. */
@Component
public class AppointmentFacts {

    public record Facts(Appointment appt, Long patientUserId, String doctorName, String hospitalName,
                        String when) {}

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy 'at' h:mm a", Locale.ENGLISH);

    private final AppointmentRepository appts;
    private final PatientRepository patients;
    private final DoctorQueryRepository doctorQuery;
    private final HospitalRepository hospitals;
    private final ZoneId zone;

    public AppointmentFacts(AppointmentRepository appts, PatientRepository patients, DoctorQueryRepository doctorQuery,
                            HospitalRepository hospitals, @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone) {
        this.appts = appts;
        this.patients = patients;
        this.doctorQuery = doctorQuery;
        this.hospitals = hospitals;
        this.zone = ZoneId.of(timezone);
    }

    public Facts load(Long appointmentId) {
        return appts.findById(appointmentId).map(this::load).orElse(null);
    }

    public Facts load(Appointment a) {
        Patient p = patients.findById(a.getPatientId()).orElse(null);
        DoctorRow d = doctorQuery.findById(a.getDoctorId(), false).orElse(null);
        Hospital h = hospitals.findById(a.getHospitalId()).orElse(null);
        ZonedDateTime when = a.getSlotStart().atZone(zone);
        return new Facts(a, p == null ? null : p.getUserId(), d == null ? "your doctor" : d.fullName(),
                h == null ? "the hospital" : h.getName(), WHEN.format(when));
    }
}