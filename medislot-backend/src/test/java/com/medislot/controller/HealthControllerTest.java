package com.medislot.controller;

import com.medislot.config.AppProperties;
import com.medislot.config.SecurityConfig;
import com.medislot.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(AppProperties.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // JwtAuthFilter (a Filter, so @WebMvcTest loads it) needs JwtService.
    // @WebMvcTest does not load @Service classes, so we mock it.
    @MockBean
    private JwtService jwtService;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void otherEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/patient/anything"))
                .andExpect(status().isUnauthorized());
    }
}