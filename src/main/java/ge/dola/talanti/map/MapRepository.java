package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MapMarkerDto;
import ge.dola.talanti.map.dto.SaveLocationDto;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
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
                .set(LOCATIONS.LATITUDE, BigDecimal.valueOf(dto.latitude()))
                .set(LOCATIONS.LONGITUDE, BigDecimal.valueOf(dto.longitude()))
                .set(LOCATIONS.ADDRESS_TEXT, dto.addressText())
                .set(LOCATIONS.CREATED_AT, LocalDateTime.now())
                .execute();
    }

    public List<MapMarkerDto> findNearbyClubs(Double lat, Double lng, Double radiusKm) {
        Field<Double> distanceKm = DSL.field(
                "6371 * acos(cos(radians({0})) * cos(radians(" + LOCATIONS.LATITUDE.getName() + ")) * " +
                        "cos(radians(" + LOCATIONS.LONGITUDE.getName() + ") - radians({1})) + " +
                        "sin(radians({0})) * sin(radians(" + LOCATIONS.LATITUDE.getName() + ")))",
                Double.class,
                lat, lng
        );

        return dsl.select(
                        CLUBS.ID.as("entityId"),
                        DSL.inline("CLUB").as("entityType"),
                        CLUBS.NAME.as("title"),
                        CLUBS.TYPE.as("subtitle"),
                        LOCATIONS.LATITUDE,
                        LOCATIONS.LONGITUDE,
                        distanceKm.as("distanceKm")
                )
                .from(CLUBS)
                .join(LOCATIONS).on(CLUBS.ID.eq(LOCATIONS.ENTITY_ID).and(LOCATIONS.ENTITY_TYPE.eq("CLUB")))
                .where(distanceKm.le(radiusKm))
                .orderBy(distanceKm.asc())
                .limit(50)
                .fetchInto(MapMarkerDto.class);
    }

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
                        CLUBS.NAME.as("subtitle"),
                        LOCATIONS.LATITUDE,
                        LOCATIONS.LONGITUDE,
                        distanceKm.as("distanceKm")
                )
                .from(TRYOUTS)
                .join(CLUBS).on(TRYOUTS.CLUB_ID.eq(CLUBS.ID))
                .join(LOCATIONS).on(TRYOUTS.LOCATION_ID.eq(LOCATIONS.ID))
                // FIX: Use startOfDay() so events scheduled for today don't vanish immediately
                .where(TRYOUTS.TRYOUT_DATE.greaterOrEqual(LocalDate.now().atStartOfDay()))
                .and(distanceKm.le(radiusKm))
                .orderBy(distanceKm.asc())
                .limit(50)
                .fetchInto(MapMarkerDto.class);
    }

    public List<MapMarkerDto> findNearbyMatches(Double lat, Double lng, Double radiusKm) {
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
                        DSL.concat(CLUBS.NAME, DSL.inline(" - "), SQUADS.NAME).as("title"),
                        MATCH_REQUESTS.STATUS.as("subtitle"),
                        LOCATIONS.LATITUDE,
                        LOCATIONS.LONGITUDE,
                        distanceKm.as("distanceKm")
                )
                .from(MATCH_REQUESTS)
                .join(CLUBS).on(MATCH_REQUESTS.CLUB_ID.eq(CLUBS.ID))
                .join(SQUADS).on(MATCH_REQUESTS.SQUAD_ID.eq(SQUADS.ID))
                .join(LOCATIONS).on(MATCH_REQUESTS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(distanceKm.le(radiusKm))
                // FIX: Filter matches so old ones don't stick around on the map
                .and(MATCH_REQUESTS.DESIRED_DATE.greaterOrEqual(LocalDate.now().atStartOfDay()))
                .orderBy(distanceKm.asc())
                .limit(50)
                .fetchInto(MapMarkerDto.class);
    }
}