package com.medislot.service;

import com.medislot.dto.StaffDtos.QrResponse;
import com.medislot.entity.Appointment;
import com.medislot.entity.Patient;
import com.medislot.exception.ApiException;
import com.medislot.repository.AppointmentRepository;
import com.medislot.repository.PatientRepository;
import com.medislot.security.AuthUser;
import com.medislot.util.SlotGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

import static com.medislot.entity.AppointmentStatus.ATTENDANCE_PENDING;
import static com.medislot.entity.AppointmentStatus.CONFIRMED;

@Service
@Transactional(readOnly = true)
public class AppointmentQrService {

    private final PatientRepository patients;
    private final AppointmentRepository appts;
    private final QrCodeService qr;
    private final ZoneId zone;

    public AppointmentQrService(PatientRepository patients, AppointmentRepository appts, QrCodeService qr,
                                @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone) {
        this.patients = patients;
        this.appts = appts;
        this.qr = qr;
        this.zone = ZoneId.of(timezone);
    }

    public QrResponse qr(AuthUser actor, Long appointmentId) {
        Patient p = patients.findByUserId(actor.id()).orElseThrow(() ->
                new ApiException(HttpStatus.FORBIDDEN, "NO_PATIENT_PROFILE", "No patient profile is linked to this account."));
        Appointment a = appts.findById(appointmentId).filter(x -> x.getPatientId().equals(p.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment not found."));
        if (a.getStatus() != CONFIRMED && a.getStatus() != ATTENDANCE_PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "QR_NOT_AVAILABLE",
                    "A QR code is available only for confirmed appointments that are not checked in yet.");
        }
        String code = qr.code(a.getId(), a.getSlotStart());
        return new QrResponse(a.getId(), code, qr.pngDataUrl(code), a.getStatus().name(), a.getAppointmentDate(),
                SlotGenerator.format(a.getSlotStart().atZone(zone).toLocalTime()));
    }
}