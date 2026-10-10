package com.medislot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilTest {

    @Test
    void londonToParisIsAbout343Km() {
        assertEquals(343.5, GeoUtil.distanceKm(51.5074, -0.1278, 48.8566, 2.3522), 2.0);
    }

    @Test
    void samePointIsZero() {
        assertEquals(0.0, GeoUtil.distanceKm(17.4126, 78.4482, 17.4126, 78.4482), 1e-9);
    }

    @Test
    void distanceIsSymmetric() {
        double ab = GeoUtil.distanceKm(17.4126, 78.4482, 17.3616, 78.4747);
        double ba = GeoUtil.distanceKm(17.3616, 78.4747, 17.4126, 78.4482);
        assertEquals(ab, ba, 1e-9);
    }

    @Test
    void boundingBoxCoversPointsInsideTheRadius() {
        double lat = 17.385;
        double lng = 78.4867;
        GeoUtil.BoundingBox box = GeoUtil.boundingBox(lat, lng, 10);
        double north = lat + Math.toDegrees(9.9 / GeoUtil.EARTH_RADIUS_KM);
        double east = lng + Math.toDegrees(9.9 / (GeoUtil.EARTH_RADIUS_KM * Math.cos(Math.toRadians(lat))));
        assertTrue(north <= box.maxLat());
        assertTrue(east <= box.maxLng());
        assertTrue(box.minLat() < lat && box.minLng() < lng);
    }

    @Test
    void coordinateValidation() {
        assertTrue(GeoUtil.isValidLatitude(90));
        assertFalse(GeoUtil.isValidLatitude(90.0001));
        assertFalse(GeoUtil.isValidLatitude(Double.NaN));
        assertTrue(GeoUtil.isValidLongitude(-180));
        assertFalse(GeoUtil.isValidLongitude(180.5));
    }
}