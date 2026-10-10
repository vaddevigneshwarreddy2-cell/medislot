package com.medislot.controller;

import com.medislot.config.SecurityConfig;
import com.medislot.security.JwtService;
import com.medislot.service.ConsultationService;
import com.medislot.service.PrescriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Clinical URLs are limited to the treating roles: only PATIENT and DOCTOR, each on their own side. */
@WebMvcTest({PatientClinicalController.class, DoctorConsultationController.class, DoctorPrescriptionController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "medislot.jwt.secret=test-secret-test-secret-test-secret-123")
class ClinicalAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private PrescriptionService prescriptionService;

    @MockBean
    private ConsultationService consultationService;

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/patient/prescriptions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/doctor/consultations/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCannotUsePatientUrls() throws Exception {
        mockMvc.perform(get("/api/patient/prescriptions")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void patientCannotUseDoctorUrls() throws Exception {
        mockMvc.perform(get("/api/doctor/prescriptions")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctor/consultations/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistHasNoClinicalAccess() throws Exception {
        mockMvc.perform(get("/api/doctor/consultations/1")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/patient/prescriptions")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HOSPITAL_ADMIN")
    void hospitalAdminHasNoClinicalAccess() throws Exception {
        mockMvc.perform(get("/api/doctor/prescriptions")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/patient/follow-ups")).andExpect(status().isForbidden());
    }
}