package com.medislot.controller;

import com.medislot.config.SecurityConfig;
import com.medislot.dto.HospitalDtos.NearbyResponse;
import com.medislot.security.JwtService;
import com.medislot.service.HospitalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HospitalController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "medislot.jwt.secret=test-secret-test-secret-test-secret-123")
class HospitalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private HospitalService hospitalService;

    @Test
    void nearbyIsPublic() throws Exception {
        when(hospitalService.nearby(anyDouble(), anyDouble(), any()))
                .thenReturn(new NearbyResponse(3.0, 0, List.of()));
        mockMvc.perform(get("/api/hospitals/nearby").param("latitude", "17.41").param("longitude", "78.44"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    void missingLatitudeIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/hospitals/nearby").param("longitude", "78.44"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
    }

    @Test
    void adminHospitalEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/admin/hospital/location"))
                .andExpect(status().isUnauthorized());
    }
}