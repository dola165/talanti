package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MapMarkerDto;
import ge.dola.talanti.map.dto.SaveLocationDto;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static ge.dola.talanti.jooq.Tables.CLUBS;
import static ge.dola.talanti.jooq.Tables.LOCATIONS;

@Repository
public class MapRepository {

    private final DSLContext dsl;

    public MapRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void saveLocation(SaveLocationDto dto) {
        dsl.insertInto(LOCATIONS)
                .set(LOCATIONS.ENTITY_TYPE, dto.entityType())
                .set(LOCATIONS.ENTITY_ID, dto.entityId())
                // Wrap the doubles in BigDecimal
                .set(LOCATIONS.LATITUDE, BigDecimal.valueOf(dto.latitude()))
                .set(LOCATIONS.LONGITUDE, BigDecimal.valueOf(dto.longitude()))
                .set(LOCATIONS.ADDRESS_TEXT, dto.addressText())
                .set(LOCATIONS.CREATED_AT, LocalDateTime.now())
                .execute();
    }

    /**
     * Finds clubs near a specific latitude/longitude using the Haversine formula.
     */
    public List<MapMarkerDto> findNearbyClubs(Double lat, Double lng, Double radiusKm) {
        // The Haversine Formula injected directly into the SQL query
        Field<Double> distanceKm = DSL.field(
                "6371 * acos(cos(radians({0})) * cos(radians(" + LOCATIONS.LATITUDE.getName() + ")) * " +
                        "cos(radians(" + LOCATIONS.LONGITUDE.getName() + ") - radians({1})) + " +
                        "sin(radians({0})) * sin(radians(" + LOCATIONS.LATITUDE.getName() + ")))",
                Double.class,
                lat, lng
        );

        return dsl.select(
                        CLUBS.ID.as("entityId"),
                        LOCATIONS.ENTITY_TYPE.as("entityType"),
                        CLUBS.NAME.as("title"),
                        CLUBS.TYPE.as("subtitle"),
                        LOCATIONS.LATITUDE,
                        LOCATIONS.LONGITUDE,
                        distanceKm.as("distanceKm")
                )
                .from(LOCATIONS)
                .join(CLUBS).on(LOCATIONS.ENTITY_ID.eq(CLUBS.ID).and(LOCATIONS.ENTITY_TYPE.eq("CLUB")))
                // Filter where distance is less than or equal to radius
                .where(distanceKm.le(radiusKm))
                // Order by closest first
                .orderBy(distanceKm.asc())
                // Limit to 50 pins so the frontend map doesn't crash
                .limit(50)
                .fetchInto(MapMarkerDto.class);
    }
}