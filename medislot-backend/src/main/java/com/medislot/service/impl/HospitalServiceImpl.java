package com.medislot.service.impl;

import com.medislot.config.AppProperties;
import com.medislot.dto.HospitalDtos.*;
import com.medislot.entity.Hospital;
import com.medislot.exception.ApiException;
import com.medislot.mapper.HospitalMapper;
import com.medislot.mapper.HospitalMapper.Core;
import com.medislot.repository.HospitalRepository;
import com.medislot.service.HospitalService;
import com.medislot.util.GeoUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class HospitalServiceImpl implements HospitalService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_NAME_RESULTS = 50;
    private static final int MAX_PAGE_SIZE = 50;

    private final HospitalRepository hospitals;
    private final HospitalMapper mapper;
    private final AppProperties props;

    public HospitalServiceImpl(HospitalRepository hospitals, HospitalMapper mapper, AppProperties props) {
        this.hospitals = hospitals;
        this.mapper = mapper;
        this.props = props;
    }

    @Override
    public SearchConfig searchConfig() {
        return new SearchConfig(props.search().defaultRadiusKm(), radiusOptions());
    }

    @Override
    public NearbyResponse nearby(double latitude, double longitude, Double radiusKm) {
        return runNearby(latitude, longitude, radiusKm, null);
    }

    @Override
    public NearbyResponse search(String name, Double latitude, Double longitude, Double radiusKm) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasLat = latitude != null;
        boolean hasLng = longitude != null;
        if (hasLat != hasLng) {
            throw invalidCoordinates();
        }
        if (!hasName && !hasLat) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SEARCH_CRITERIA_REQUIRED",
                    "Provide a hospital name, a location, or both.");
        }
        if (hasLat) {
            return runNearby(latitude, longitude, radiusKm, hasName ? name : null);
        }
        Page<Hospital> page = hospitals.searchByName(namePattern(name),
                PageRequest.of(0, MAX_NAME_RESULTS, Sort.by("name")));
        List<Summary> result = mapper.toSummaries(page.getContent().stream().map(HospitalMapper::core).toList());
        return new NearbyResponse(null, result.size(), result);
    }

    @Override
    public PageResponse<Summary> list(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGE",
                    "page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE + ".");
        }
        Page<Hospital> p = hospitals.searchByName("%", PageRequest.of(page, size, Sort.by("name")));
        List<Summary> items = mapper.toSummaries(p.getContent().stream().map(HospitalMapper::core).toList());
        return new PageResponse<>(items, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Override
    public Detail get(Long id, Double latitude, Double longitude) {
        Hospital h = hospitals.findByIdAndActiveTrue(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "HOSPITAL_NOT_FOUND", "Hospital not found."));
        Double distance = null;
        if (latitude != null || longitude != null) {
            if (latitude == null || longitude == null) throw invalidCoordinates();
            validateCoordinates(latitude, longitude);
            distance = GeoUtil.distanceKm(latitude, longitude,
                    h.getLatitude().doubleValue(), h.getLongitude().doubleValue());
        }
        return mapper.toDetail(h, distance);
    }

    // ---------- helpers ----------

    private NearbyResponse runNearby(double lat, double lng, Double radiusKm, String name) {
        validateCoordinates(lat, lng);
        double radius = resolveRadius(radiusKm);
        GeoUtil.BoundingBox box = GeoUtil.boundingBox(lat, lng, radius);
        List<HospitalRepository.NearbyRow> rows = hospitals.findNearby(lat, lng,
                box.minLat(), box.maxLat(), box.minLng(), box.maxLng(), namePattern(name), radius);
        List<Core> cores = rows.stream().map(r -> new Core(r.getId(), r.getName(), r.getAddressLine(),
                r.getCity(), r.getState(), r.getLatitude(), r.getLongitude(), r.getPhone(),
                r.getDistanceKm())).toList();
        List<Summary> result = mapper.toSummaries(cores);
        return new NearbyResponse(radius, result.size(), result);
    }

    private void validateCoordinates(double lat, double lng) {
        if (!GeoUtil.isValidLatitude(lat) || !GeoUtil.isValidLongitude(lng)) {
            throw invalidCoordinates();
        }
    }

    private ApiException invalidCoordinates() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COORDINATES",
                "Provide both latitude (-90 to 90) and longitude (-180 to 180).");
    }

    private double resolveRadius(Double requested) {
        double radius = requested == null ? props.search().defaultRadiusKm() : requested;
        int max = Collections.max(radiusOptions());
        if (!Double.isFinite(radius) || radius <= 0 || radius > max) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RADIUS",
                    "Search radius must be greater than 0 and at most " + max + " km.");
        }
        return radius;
    }

    private List<Integer> radiusOptions() {
        List<Integer> options = props.search().radiusOptionsKm();
        return (options == null || options.isEmpty()) ? List.of(1, 3, 5, 10, 25, 50) : options;
    }

    private String namePattern(String name) {
        if (name == null || name.isBlank()) return "%";
        String n = name.trim();
        if (n.length() > MAX_NAME_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_NAME", "The hospital name is too long.");
        }
        String escaped = n.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }
}