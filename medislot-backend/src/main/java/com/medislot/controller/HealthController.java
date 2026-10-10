package com.medislot.controller;

import com.medislot.config.AppProperties;
import com.medislot.dto.ApiResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final ObjectProvider<JdbcTemplate> jdbcTemplate;
    private final AppProperties props;

    public HealthController(ObjectProvider<JdbcTemplate> jdbcTemplate, AppProperties props) {
        this.jdbcTemplate = jdbcTemplate;
        this.props = props;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("database", databaseStatus());
        data.put("defaultSearchRadiusKm", props.search().defaultRadiusKm());
        data.put("maxActiveAppointments", props.booking().maxActiveAppointments());
        return ApiResponse.ok("MediSlot API is running", data);
    }

    private String databaseStatus() {
        JdbcTemplate jt = jdbcTemplate.getIfAvailable();
        if (jt == null) return "UNKNOWN";
        try {
            jt.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}