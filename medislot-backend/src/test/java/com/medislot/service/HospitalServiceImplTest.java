package com.medislot.service;

import com.medislot.config.AppProperties;
import com.medislot.dto.HospitalDtos.NearbyResponse;
import com.medislot.exception.ApiException;
import com.medislot.mapper.HospitalMapper;
import com.medislot.repository.HospitalRepository;
import com.medislot.service.impl.HospitalServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HospitalServiceImplTest {

    private final HospitalRepository repo = mock(HospitalRepository.class);
    private final HospitalMapper mapper = mock(HospitalMapper.class);
    private final AppProperties props = new AppProperties(
            new AppProperties.Search(3, List.of(1, 3, 5, 10, 25, 50)), null, null, null, null);
    private final HospitalServiceImpl service = new HospitalServiceImpl(repo, mapper, props);

    private String codeOf(Runnable r) {
        return assertThrows(ApiException.class, r::run).getCode();
    }

    @Test
    void zeroRadiusIsRejected() {
        assertEquals("INVALID_RADIUS", codeOf(() -> service.nearby(17.4, 78.4, 0.0)));
    }

    @Test
    void radiusAboveMaximumIsRejected() {
        assertEquals("INVALID_RADIUS", codeOf(() -> service.nearby(17.4, 78.4, 100.0)));
    }

    @Test
    void invalidLatitudeIsRejected() {
        assertEquals("INVALID_COORDINATES", codeOf(() -> service.nearby(91.0, 78.4, 3.0)));
    }

    @Test
    void searchNeedsAtLeastOneCriterion() {
        assertEquals("SEARCH_CRITERIA_REQUIRED", codeOf(() -> service.search(" ", null, null, null)));
    }

    @Test
    void latitudeWithoutLongitudeIsRejected() {
        assertEquals("INVALID_COORDINATES", codeOf(() -> service.search("x", 17.4, null, 3.0)));
    }

    @Test
    void missingRadiusUsesConfiguredDefault() {
        NearbyResponse r = service.nearby(17.4, 78.4, null);
        assertEquals(3.0, r.radiusKm());
        assertEquals(0, r.count());
        verify(repo).findNearby(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                anyDouble(), eq("%"), eq(3.0));
    }
}