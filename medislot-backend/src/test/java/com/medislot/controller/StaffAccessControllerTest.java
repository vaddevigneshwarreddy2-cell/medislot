package com.medislot.controller;

import com.medislot.config.SecurityConfig;
import com.medislot.security.JwtService;
import com.medislot.service.AdminScope;
import com.medislot.service.StaffAccountService;
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

@WebMvcTest({AdminStaffController.class, SuperAdminDirectoryController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "medislot.jwt.secret=test-secret-test-secret-test-secret-123")
class StaffAccessControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private JwtService jwtService;
    @MockBean private StaffAccountService staffAccountService;
    @MockBean private AdminScope adminScope;

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/staff")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/super-admin/hospitals")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistCannotManageStaff() throws Exception {
        mockMvc.perform(get("/api/admin/staff")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HOSPITAL_ADMIN")
    void hospitalAdminCannotUseSuperAdminUrls() throws Exception {
        mockMvc.perform(get("/api/super-admin/hospitals")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/super-admin/hospitals/1/admins")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void superAdminCanListHospitals() throws Exception {
        mockMvc.perform(get("/api/super-admin/hospitals")).andExpect(status().isOk());
    }
}