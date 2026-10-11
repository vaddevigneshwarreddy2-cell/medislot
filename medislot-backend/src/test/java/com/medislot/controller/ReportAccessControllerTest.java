package com.medislot.controller;

import com.medislot.config.SecurityConfig;
import com.medislot.security.JwtService;
import com.medislot.service.AuditQueryService;
import com.medislot.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ReportController.class, SuperAdminAuditController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "medislot.jwt.secret=test-secret-test-secret-test-secret-123")
class ReportAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private ReportService reportService;

    @MockBean
    private AuditQueryService auditQueryService;

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/reports")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/super-admin/audit-logs")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void patientIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/reports")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/reports/export")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/super-admin/audit-logs")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorAndReceptionistAreForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/reports")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HOSPITAL_ADMIN")
    void hospitalAdminCannotReadThePlatformAuditLog() throws Exception {
        mockMvc.perform(get("/api/super-admin/audit-logs")).andExpect(status().isForbidden());
    }
}