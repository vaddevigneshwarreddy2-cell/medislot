package com.medislot.controller;

import com.medislot.config.SecurityConfig;
import com.medislot.security.JwtService;
import com.medislot.service.AttendanceService;
import com.medislot.service.DashboardService;
import com.medislot.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({NotificationController.class, DashboardController.class, AttendanceController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "medislot.jwt.secret=test-secret-test-secret-test-secret-123")
class NotificationAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private AttendanceService attendanceService;

    @Test
    void anonymousCannotReadNotificationsOrDashboards() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/patient/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void patientCannotUseStaffDashboards() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctor/dashboard")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reception/attention")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCannotUsePatientDashboardOrAttendance() throws Exception {
        mockMvc.perform(get("/api/patient/dashboard")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/appointments/1/attendance")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistCannotUseAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isForbidden());
    }
}