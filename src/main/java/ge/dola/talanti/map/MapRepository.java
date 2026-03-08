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

import static ge.dola.talanti.jooq.Tables.*;
import static ge.dola.talanti.jooq.tables.Tryouts.TRYOUTS;

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

    /**
     * Finds active tryouts near a specific latitude/longitude using the Haversine formula.
     */
    public List<MapMarkerDto> findNearbyTryouts(Double lat, Double lng, Double radiusKm) {
        Field<Double> distanceKm = DSL.field(
                "6371 * acos(cos(radians({0})) * cos(radians(" + LOCATIONS.LATITUDE.getName() + ")) * " +
                        "cos(radians(" + LOCATIONS.LONGITUDE.getName() + ") - radians({1})) + " +
                        "sin(radians({0})) * sin(radians(" + LOCATIONS.LATITUDE.getName() + ")))",
                Double.class,
                lat, lng
        );

        return dsl.select(
                        TRYOUTS.ID.as("entityId"),
                        DSL.inline("TRYOUT").as("entityType"),
                        TRYOUTS.TITLE.as("title"),
                        CLUBS.NAME.as("subtitle"), // This makes the club name show up under the tryout title
                        LOCATIONS.LATITUDE,
                        LOCATIONS.LONGITUDE,
                        distanceKm.as("distanceKm")
                )
                .from(TRYOUTS)
                .join(CLUBS).on(TRYOUTS.CLUB_ID.eq(CLUBS.ID))
                .join(LOCATIONS).on(TRYOUTS.LOCATION_ID.eq(LOCATIONS.ID))
                // Only show tryouts that haven't happened yet
                .where(TRYOUTS.TRYOUT_DATE.greaterOrEqual(LocalDateTime.now()))
                // Only show within the slider's radius
                .and(distanceKm.le(radiusKm))
                .orderBy(distanceKm.asc())
                .limit(50)
                .fetchInto(MapMarkerDto.class);
    }


    /**
     * Finds OPEN Match Requests near a specific latitude/longitude using the Haversine formula.
     */
    public List<MapMarkerDto> findNearbyMatchRequests(Double lat, Double lng, Double radiusKm) {
        Field<Double> distanceKm = DSL.field(
                "6371 * acos(cos(radians({0})) * cos(radians(" + LOCATIONS.LATITUDE.getName() + ")) * " +
                        "cos(radians(" + LOCATIONS.LONGITUDE.getName() + ") - radians({1})) + " +
                        "sin(radians({0})) * sin(radians(" + LOCATIONS.LATITUDE.getName() + ")))",
                Double.class,
                lat, lng
        );

        return dsl.select(
                        MATCH_REQUESTS.ID.as("entityId"),
                        DSL.inline("MATCH_REQUEST").as("entityType"),
                        // Combines Club Name + Squad Category (e.g. "Dinamo FC (U16)")
                        DSL.concat(CLUBS.NAME, DSL.val(" ("), SQUADS.CATEGORY, DSL.val(")")).as("title"),
                        MATCH_REQUESTS.LOCATION_PREF.as("subtitle"), // "CAN_HOST"
                        LOCATIONS.LATITUDE,
                        LOCATIONS.LONGITUDE,
                        distanceKm.as("distanceKm")
                )
                .from(MATCH_REQUESTS)
                .join(CLUBS).on(MATCH_REQUESTS.CLUB_ID.eq(CLUBS.ID))
                .join(SQUADS).on(MATCH_REQUESTS.SQUAD_ID.eq(SQUADS.ID))
                .join(LOCATIONS).on(MATCH_REQUESTS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(MATCH_REQUESTS.STATUS.eq("OPEN"))
                // Only show games in the future
                .and(MATCH_REQUESTS.DESIRED_DATE.greaterOrEqual(LocalDateTime.now()))
                .and(distanceKm.le(radiusKm))
                .orderBy(distanceKm.asc())
                .limit(50)
                .fetchInto(MapMarkerDto.class);
    }
}