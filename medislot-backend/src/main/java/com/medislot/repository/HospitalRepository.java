package com.medislot.repository;

import com.medislot.entity.Hospital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    /** One row of a nearby search. Aliases in the native query must match these getters. */
    interface NearbyRow {
        Long getId();
        String getName();
        String getAddressLine();
        String getCity();
        String getState();
        BigDecimal getLatitude();
        BigDecimal getLongitude();
        String getPhone();
        Double getDistanceKm();
    }

    interface DoctorCountRow {
        Long getHospitalId();
        Long getTotal();
    }

    Optional<Hospital> findByIdAndActiveTrue(Long id);

    /** pattern is a lower-case LIKE pattern using '!' as the escape character. */
    @Query("select h from Hospital h where h.active = true and lower(h.name) like :pattern escape '!'")
    Page<Hospital> searchByName(@Param("pattern") String pattern, Pageable pageable);

    /**
     * Haversine distance computed in PostgreSQL. The bounding box lets the lat/lng index
     * discard far-away rows first; the exact distance check is the real filter.
     */
    @Query(value = """
            SELECT * FROM (
              SELECT h.id AS "id", h.name AS "name", h.address_line AS "addressLine",
                     h.city AS "city", h.state AS "state",
                     h.latitude AS "latitude", h.longitude AS "longitude", h.phone AS "phone",
                     2 * 6371.0 * asin(sqrt(least(1.0,
                         power(sin(radians(CAST(h.latitude AS double precision) - :lat) / 2), 2)
                       + cos(radians(:lat)) * cos(radians(CAST(h.latitude AS double precision)))
                         * power(sin(radians(CAST(h.longitude AS double precision) - :lng) / 2), 2)
                     ))) AS "distanceKm"
              FROM hospitals h
              WHERE h.active = TRUE
                AND h.latitude BETWEEN :minLat AND :maxLat
                AND h.longitude BETWEEN :minLng AND :maxLng
                AND lower(h.name) LIKE :pattern ESCAPE '!'
            ) t
            WHERE t."distanceKm" <= :radius
            ORDER BY t."distanceKm" ASC, t."name" ASC
            """, nativeQuery = true)
    List<NearbyRow> findNearby(@Param("lat") double lat, @Param("lng") double lng,
                               @Param("minLat") double minLat, @Param("maxLat") double maxLat,
                               @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                               @Param("pattern") String pattern, @Param("radius") double radius);

    @Query(value = """
            SELECT d.hospital_id AS "hospitalId", COUNT(*) AS "total"
            FROM doctors d
            WHERE d.hospital_id IN (:ids) AND d.active = TRUE
            GROUP BY d.hospital_id
            """, nativeQuery = true)
    List<DoctorCountRow> countActiveDoctors(@Param("ids") Collection<Long> ids);
}