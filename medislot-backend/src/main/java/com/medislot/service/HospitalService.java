package com.medislot.service;

import com.medislot.dto.HospitalDtos.*;

/** Public hospital discovery. */
public interface HospitalService {
    SearchConfig searchConfig();
    NearbyResponse nearby(double latitude, double longitude, Double radiusKm);
    NearbyResponse search(String name, Double latitude, Double longitude, Double radiusKm);
    PageResponse<Summary> list(int page, int size);
    Detail get(Long id, Double latitude, Double longitude);
}