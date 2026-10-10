package com.medislot.service;

import com.medislot.dto.ClinicalDtos.*;
import com.medislot.entity.Patient;
import com.medislot.exception.ApiException;
import com.medislot.repository.*;
import com.medislot.security.AuthUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Read access to finalized prescriptions: the patient who owns them, or the doctor who issued them. */
@Service
@Transactional(readOnly = true)
public class PrescriptionService {

    private static final int LIMIT = 200;

    private final ClinicalQueryRepository clinical;
    private final PrescriptionItemRepository items;
    private final PatientRepository patients;
    private final DoctorAccess access;
    private final PrescriptionPdfService pdf;
    private final AuditService audit;
    private final ZoneId zone;

    public PrescriptionService(ClinicalQueryRepository clinical, PrescriptionItemRepository items,
                               PatientRepository patients, DoctorAccess access, PrescriptionPdfService pdf,
                               AuditService audit, @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone) {
        this.clinical = clinical;
        this.items = items;
        this.patients = patients;
        this.access = access;
        this.pdf = pdf;
        this.audit = audit;
        this.zone = ZoneId.of(timezone);
    }

    // ---------- patient ----------

    public List<PrescriptionSummary> listForPatient(AuthUser actor) {
        return summaries(clinical.search(new ClinicalQueryRepository.Filter(null, patient(actor).getId(), null), LIMIT));
    }

    public PrescriptionDetail detailForPatient(AuthUser actor, Long id) {
        return detail(rowForPatient(actor, id));
    }

    @Transactional
    public byte[] pdfForPatient(AuthUser actor, Long id) {
        PrescriptionRow r = rowForPatient(actor, id);
        audit.log(actor.id(), actor.email(), "PRESCRIPTION_DOWNLOADED", "PRESCRIPTION", String.valueOf(id), null, null);
        return pdf.render(detail(r));
    }

    public List<FollowUp> followUps(AuthUser actor) {
        return clinical.followUps(patient(actor).getId(), LocalDate.now(zone)).stream()
                .map(f -> new FollowUp(f.appointmentId(), f.followUpDate(), f.doctorId(), f.doctorName(),
                        f.specialization(), f.hospitalName())).toList();
    }

    // ---------- doctor ----------

    public List<PrescriptionSummary> listForDoctor(AuthUser actor) {
        return summaries(clinical.search(new ClinicalQueryRepository.Filter(null, null, access.forSelf(actor).getId()), LIMIT));
    }

    public PrescriptionDetail detailForDoctor(AuthUser actor, Long id) {
        return detail(rowForDoctor(actor, id));
    }

    @Transactional
    public byte[] pdfForDoctor(AuthUser actor, Long id) {
        PrescriptionRow r = rowForDoctor(actor, id);
        audit.log(actor.id(), actor.email(), "PRESCRIPTION_DOWNLOADED", "PRESCRIPTION", String.valueOf(id), null, null);
        return pdf.render(detail(r));
    }

    // ---------- helpers ----------

    private Patient patient(AuthUser actor) {
        return patients.findByUserId(actor.id()).orElseThrow(() ->
                new ApiException(HttpStatus.FORBIDDEN, "NO_PATIENT_PROFILE", "No patient profile is linked to this account."));
    }

    private PrescriptionRow rowForPatient(AuthUser actor, Long id) {
        return clinical.search(new ClinicalQueryRepository.Filter(id, patient(actor).getId(), null), 1)
                .stream().findFirst().orElseThrow(PrescriptionService::notFound);
    }

    private PrescriptionRow rowForDoctor(AuthUser actor, Long id) {
        return clinical.search(new ClinicalQueryRepository.Filter(id, null, access.forSelf(actor).getId()), 1)
                .stream().findFirst().orElseThrow(PrescriptionService::notFound);
    }

    private List<PrescriptionSummary> summaries(List<PrescriptionRow> rows) {
        Map<Long, Integer> counts = new HashMap<>();
        if (!rows.isEmpty()) {
            for (Object[] o : items.countByPrescription(rows.stream().map(PrescriptionRow::id).toList())) {
                counts.put((Long) o[0], ((Number) o[1]).intValue());
            }
        }
        return rows.stream().map(r -> new PrescriptionSummary(r.id(), r.appointmentId(), r.date(), r.doctorName(),
                r.specialization(), r.hospitalName(), counts.getOrDefault(r.id(), 0), r.followUpDate(),
                r.patientName())).toList();
    }

    private PrescriptionDetail detail(PrescriptionRow r) {
        List<MedicineView> meds = items.findByPrescriptionIdOrderBySortOrderAsc(r.id()).stream()
                .map(i -> new MedicineView(i.getMedicineName(), i.getDosage(), i.getFrequency(), i.getDuration(),
                        i.getInstructions())).toList();
        return new PrescriptionDetail(r.id(), r.appointmentId(), r.date(), r.doctorName(), r.specialization(),
                r.qualification(), r.hospitalName(), r.hospitalAddress() + ", " + r.hospitalCity(),
                r.patientName(), r.diagnosis(), r.instructions(), r.followUpDate(), meds);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "PRESCRIPTION_NOT_FOUND", "Prescription not found.");
    }
}