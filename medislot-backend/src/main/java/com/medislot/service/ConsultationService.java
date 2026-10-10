package com.medislot.service;

import com.medislot.dto.ClinicalDtos.*;
import com.medislot.entity.*;
import com.medislot.exception.ApiException;
import com.medislot.repository.*;
import com.medislot.security.AuthUser;
import com.medislot.util.SlotGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.medislot.entity.AppointmentStatus.COMPLETED;
import static com.medislot.entity.AppointmentStatus.IN_CONSULTATION;

/**
 * The treating doctor's consultation: draft, then complete. Every method resolves the doctor from the
 * login and loads the appointment only if it belongs to that doctor, so other doctors get 404.
 */
@Service
public class ConsultationService {

    private final DoctorAccess access;
    private final DoctorRepository doctors;
    private final AppointmentRepository appts;
    private final ConsultationRepository consultations;
    private final PrescriptionRepository prescriptions;
    private final PrescriptionItemRepository items;
    private final StaffQueryRepository staffQuery;
    private final AppointmentStateMachine machine;
    private final AuditService audit;
    private final ZoneId zone;

    public ConsultationService(DoctorAccess access, DoctorRepository doctors, AppointmentRepository appts,
                               ConsultationRepository consultations, PrescriptionRepository prescriptions,
                               PrescriptionItemRepository items, StaffQueryRepository staffQuery,
                               AppointmentStateMachine machine, AuditService audit,
                               @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone) {
        this.access = access;
        this.doctors = doctors;
        this.appts = appts;
        this.consultations = consultations;
        this.prescriptions = prescriptions;
        this.items = items;
        this.staffQuery = staffQuery;
        this.machine = machine;
        this.audit = audit;
        this.zone = ZoneId.of(timezone);
    }

    @Transactional(readOnly = true)
    public ConsultationView get(AuthUser actor, Long appointmentId) {
        Doctor d = access.forSelf(actor);
        Appointment a = appts.findById(appointmentId).filter(x -> x.getDoctorId().equals(d.getId()))
                .orElseThrow(ConsultationService::notFound);
        if (a.getStatus() != IN_CONSULTATION && a.getStatus() != COMPLETED) {
            throw noConsultation();
        }
        return view(d, a);
    }

    @Transactional
    public ConsultationView save(AuthUser actor, Long appointmentId, ConsultationRequest r) {
        Doctor d = access.forSelf(actor);
        Appointment a = lockOwned(d, appointmentId);
        requireEditable(a);
        upsert(a, d, r);
        return view(d, a);
    }

    @Transactional
    public ConsultationView complete(AuthUser actor, Long appointmentId, ConsultationRequest r) {
        Doctor d = access.forSelf(actor);
        Appointment a = lockOwned(d, appointmentId);
        requireEditable(a);
        ClinicalRules.requireContent(r.notes(), r.diagnosis());

        Consultation c = upsert(a, d, r);
        Instant now = Instant.now();
        machine.move(a, COMPLETED, actor.id(), "Consultation completed");
        a.setCompletedAt(now);
        c.setFinalized(true);
        c.setFinalizedAt(now);

        audit.log(actor.id(), actor.email(), "CONSULTATION_COMPLETED", "APPOINTMENT",
                String.valueOf(a.getId()), null, null);
        prescriptions.findByConsultationId(c.getId()).ifPresent(p ->
                audit.log(actor.id(), actor.email(), "PRESCRIPTION_CREATED", "PRESCRIPTION",
                        String.valueOf(p.getId()), "appointmentId=" + a.getId(), null));
        return view(d, a);
    }

    // ---------- helpers ----------

    private Appointment lockOwned(Doctor d, Long appointmentId) {
        doctors.findByIdForUpdate(d.getId()); // lock order everywhere: doctor, then appointment
        return appts.findByIdForUpdate(appointmentId).filter(x -> x.getDoctorId().equals(d.getId()))
                .orElseThrow(ConsultationService::notFound);
    }

    private void requireEditable(Appointment a) {
        if (a.getStatus() == COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "CONSULTATION_LOCKED",
                    "This consultation is completed and can no longer be edited.");
        }
        if (a.getStatus() != IN_CONSULTATION) throw noConsultation();
    }

    private Consultation upsert(Appointment a, Doctor d, ConsultationRequest r) {
        ClinicalRules.validateFollowUp(a.getAppointmentDate(), r.followUpDate());

        Consultation c = consultations.findByAppointmentId(a.getId()).orElseGet(() -> {
            Consultation n = new Consultation();
            n.setAppointmentId(a.getId());
            n.setDoctorId(d.getId());
            n.setPatientId(a.getPatientId());
            n.setHospitalId(a.getHospitalId());
            return consultations.save(n);
        });
        c.setNotes(blankToNull(r.notes()));
        c.setDiagnosis(blankToNull(r.diagnosis()));
        c.setInstructions(blankToNull(r.instructions()));
        c.setFollowUpDate(r.followUpDate());

        List<MedicineInput> meds = r.medicines() == null ? List.of() : r.medicines();
        Prescription p = prescriptions.findByConsultationId(c.getId()).orElse(null);
        if (meds.isEmpty()) {
            if (p != null) {
                items.deleteAllForPrescription(p.getId());
                prescriptions.delete(p);
            }
        } else {
            if (p == null) {
                Prescription n = new Prescription();
                n.setConsultationId(c.getId());
                p = prescriptions.save(n);
            } else {
                items.deleteAllForPrescription(p.getId());
            }
            List<PrescriptionItem> rows = new ArrayList<>();
            int order = 0;
            for (MedicineInput m : meds) {
                PrescriptionItem i = new PrescriptionItem();
                i.setPrescriptionId(p.getId());
                i.setSortOrder(order++);
                i.setMedicineName(m.medicineName().trim());
                i.setDosage(m.dosage().trim());
                i.setFrequency(m.frequency().trim());
                i.setDuration(m.duration().trim());
                i.setInstructions(blankToNull(m.instructions()));
                rows.add(i);
            }
            items.saveAll(rows);
        }
        return c;
    }

    private ConsultationView view(Doctor d, Appointment a) {
        StaffRow row = staffQuery.search(new StaffQueryRepository.Filter(d.getHospitalId(), d.getId(), null, null,
                null, a.getId()), 1).stream().findFirst().orElseThrow(ConsultationService::notFound);
        Consultation c = consultations.findByAppointmentId(a.getId()).orElse(null);
        Prescription p = c == null ? null : prescriptions.findByConsultationId(c.getId()).orElse(null);
        List<MedicineView> meds = p == null ? List.of()
                : items.findByPrescriptionIdOrderBySortOrderAsc(p.getId()).stream()
                .map(i -> new MedicineView(i.getMedicineName(), i.getDosage(), i.getFrequency(), i.getDuration(),
                        i.getInstructions())).toList();
        boolean finalized = c != null && c.isFinalized();
        return new ConsultationView(a.getId(), a.getStatus().name(), row.patientName(), row.reason(),
                a.getAppointmentDate(), SlotGenerator.format(a.getSlotStart().atZone(zone).toLocalTime()),
                c == null ? null : c.getNotes(), c == null ? null : c.getDiagnosis(),
                c == null ? null : c.getInstructions(), c == null ? null : c.getFollowUpDate(),
                meds, finalized, a.getStatus() == IN_CONSULTATION, finalized && p != null ? p.getId() : null);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment not found.");
    }

    private static ApiException noConsultation() {
        return new ApiException(HttpStatus.CONFLICT, "NO_CONSULTATION",
                "Start the consultation from the queue first.");
    }
}