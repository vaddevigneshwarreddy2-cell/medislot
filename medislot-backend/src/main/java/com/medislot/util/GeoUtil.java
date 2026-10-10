package com.medislot.util;

/** Geographic helpers (Haversine distance, bounding box, coordinate validation). */
public final class GeoUtil {

    public static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtil() {}

    public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {}

    public static boolean isValidLatitude(double v) {
        return Double.isFinite(v) && v >= -90.0 && v <= 90.0;
    }

    public static boolean isValidLongitude(double v) {
        return Double.isFinite(v) && v >= -180.0 && v <= 180.0;
    }

    /** Great-circle distance in kilometres. */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(Math.min(1.0, a)));
    }

    /**
     * Rectangle guaranteed to contain every point within radiusKm of the centre.
     * Near the poles or the antimeridian it widens to the full longitude range.
     */
    public static BoundingBox boundingBox(double lat, double lng, double radiusKm) {
        double angular = radiusKm / EARTH_RADIUS_KM;
        double latDelta = Math.toDegrees(angular) * 1.001;
        double minLat = Math.max(-90.0, lat - latDelta);
        double maxLat = Math.min(90.0, lat + latDelta);
        double minLng = -180.0;
        double maxLng = 180.0;
        if (Math.abs(lat) + latDelta < 89.0) {
            double lngDelta = Math.toDegrees(Math.asin(Math.sin(angular) / Math.cos(Math.toRadians(lat)))) * 1.001;
            if (lng - lngDelta >= -180.0 && lng + lngDelta <= 180.0) {
                minLng = lng - lngDelta;
                maxLng = lng + lngDelta;
            }
        }
        return new BoundingBox(minLat, maxLat, minLng, maxLng);
    }
}