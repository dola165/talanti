package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MapMarkerDto;
import ge.dola.talanti.map.dto.SaveLocationDto;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static ge.dola.talanti.jooq.Tables.*;
import org.jooq.Records;

@Repository
public class MapRepository {

    private final DSLContext dsl;

    public MapRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Long saveLocation(SaveLocationDto dto) {
        // Relational Fix: No more polymorphic entity_type/entity_id.
        // Just saves the location and returns the ID for the caller to link.
        return dsl.insertInto(LOCATIONS)
                .set(LOCATIONS.LATITUDE, BigDecimal.valueOf(dto.latitude()))
                .set(LOCATIONS.LONGITUDE, BigDecimal.valueOf(dto.longitude()))
                .set(LOCATIONS.ADDRESS_TEXT, dto.addressText())
                .set(LOCATIONS.CREATED_AT, LocalDateTime.now())
                .returningResult(LOCATIONS.ID)
                .fetchOneInto(Long.class);
    }

    private Condition buildLocationCondition(Condition base, List<String> cities, List<String> countries) {
        Condition updated = base;

        if (cities != null && !cities.isEmpty()) {
            Condition cityCond = DSL.falseCondition();
            for (String c : cities) {
                cityCond = cityCond.or(LOCATIONS.ADDRESS_TEXT.containsIgnoreCase(c));
            }
            updated = updated.and(cityCond);
        }

        if (countries != null && !countries.isEmpty()) {
            Condition countryCond = DSL.falseCondition();
            for (String c : countries) {
                countryCond = countryCond.or(LOCATIONS.ADDRESS_TEXT.containsIgnoreCase(c));
            }
            updated = updated.and(countryCond);
        }
        return updated;
    }

    // --- SAFE HAVERSINE IMPLEMENTATION ---
    private Field<Double> getHaversineFormula(Double lat, Double lng) {
        // STRICT ENFORCEMENT: Cast to float/double inside the SQL to satisfy Postgres Math requirements
        return DSL.field(
                "6371 * acos(" +
                        "cos(radians({0})) * cos(radians({1}::float)) * " +
                        "cos(radians({2}::float) - radians({3})) + " +
                        "sin(radians({0})) * sin(radians({1}::float))" +
                        ")",
                Double.class,
                lat,
                LOCATIONS.LATITUDE,
                LOCATIONS.LONGITUDE,
                lng
        );
    }


    public List<MapMarkerDto> findNearbyClubs(Double lat, Double lng, Double radiusKm, List<String> cities, List<String> countries) {
        // 1. The raw math formula (Must be used for WHERE)
        Field<Double> distanceMath = getHaversineFormula(lat, lng);

        // 2. The alias (Must ONLY be used in SELECT)
        Field<Double> distanceKm = distanceMath.as("distanceKm");

        // STRICT ENFORCEMENT: Pass the raw math field into your condition builder
        Condition conditions = buildLocationCondition(distanceMath.le(radiusKm), cities, countries);

        return dsl.select(
                        CLUBS.ID.as("entityId"),
                        DSL.inline("CLUB").as("entityType"),
                        CLUBS.NAME.as("title"),
                        CLUBS.TYPE.as("subtitle"),
                        LOCATIONS.LATITUDE.cast(Double.class).as("latitude"), // STRICT CAST
                        LOCATIONS.LONGITUDE.cast(Double.class).as("longitude"), // STRICT CAST
                        distanceKm, // Use the alias here
                        DSL.field(DSL.selectCount().from(CLUB_MEMBERSHIPS).where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))).as("members"),
                        DSL.field(DSL.selectCount().from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID))).as("followers"),
                        CLUBS.STATUS.eq("VERIFIED").as("verified"), // Adjusted to use your VERIFIED status logic
                        DSL.inline("").as("date"),
                        DSL.inline("").as("fee")
                )
                .from(CLUBS)
                .join(LOCATIONS).on(CLUBS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(conditions)
                // Natively maps to the Record constructor without reflection
                .fetch(Records.mapping(MapMarkerDto::new));
    }

    public List<MapMarkerDto> findNearbyTryouts(Double lat, Double lng, Double radiusKm, List<String> gender, List<String> ageGroups, List<String> cities, List<String> countries) {
        // 1. The raw math formula (Must be used for WHERE)
        Field<Double> distanceMath = getHaversineFormula(lat, lng);

        // 2. The alias (Must ONLY be used in SELECT)
        Field<Double> distanceKm = distanceMath.as("distanceKm");

        // STRICT ENFORCEMENT: Pass the raw math field into your condition builder
        Condition conditions = buildLocationCondition(distanceMath.le(radiusKm), cities, countries);

        if (ageGroups != null && !ageGroups.isEmpty()) {
            conditions = conditions.and(TRYOUTS.AGE_GROUP.in(ageGroups));
        }

        return dsl.select(
                        TRYOUTS.ID.as("entityId"),
                        DSL.inline("TRYOUT").as("entityType"),
                        TRYOUTS.TITLE.as("title"),
                        TRYOUTS.POSITION.as("subtitle"),
                        LOCATIONS.LATITUDE.cast(Double.class).as("latitude"),
                        LOCATIONS.LONGITUDE.cast(Double.class).as("longitude"),
                        distanceKm.as("distanceKm"),
                        DSL.inline(0).as("members"),
                        DSL.inline(0).as("followers"),
                        DSL.inline(false).as("verified"),
                        DSL.cast(TRYOUTS.TRYOUT_DATE, String.class).as("date"),
                        DSL.inline("Free").as("fee")
                )
                .from(TRYOUTS)
                .join(LOCATIONS).on(TRYOUTS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(conditions)
                .fetch(Records.mapping(MapMarkerDto::new));
    }

    public List<MapMarkerDto> findNearbyMatches(Double lat, Double lng, Double radiusKm, List<String> gender, List<String> ageGroups, List<String> cities, List<String> countries) {
        // 1. The raw math formula (Must be used for WHERE)
        Field<Double> distanceMath = getHaversineFormula(lat, lng);

        // 2. The alias (Must ONLY be used in SELECT)
        Field<Double> distanceKm = distanceMath.as("distanceKm");

        // STRICT ENFORCEMENT: Pass the raw math field into your condition builder
        Condition conditions = buildLocationCondition(distanceMath.le(radiusKm), cities, countries);
        conditions = conditions.and(MATCHES.SCHEDULED_DATE.greaterOrEqual(LocalDateTime.now()));
        conditions = conditions.and(MATCHES.STATUS.eq("OPEN"));

        return dsl.select(
                        MATCHES.ID.as("entityId"),
                        DSL.inline("MATCH").as("entityType"),
                        CLUBS.NAME.as("title"),
                        MATCHES.STATUS.as("subtitle"),
                        LOCATIONS.LATITUDE.cast(Double.class).as("latitude"),
                        LOCATIONS.LONGITUDE.cast(Double.class).as("longitude"),
                        distanceKm.as("distanceKm"),
                        DSL.inline(0).as("members"),
                        DSL.inline(0).as("followers"),
                        DSL.inline(false).as("verified"),
                        DSL.cast(MATCHES.SCHEDULED_DATE, String.class).as("date"),
                        DSL.inline("Free").as("fee")
                )
                .from(MATCHES)
                .join(CLUBS).on(MATCHES.HOME_CLUB_ID.eq(CLUBS.ID))
                .join(LOCATIONS).on(LOCATIONS.ID.eq(DSL.coalesce(MATCHES.LOCATION_ID, CLUBS.LOCATION_ID)))
                .where(conditions)
                .fetch(Records.mapping(MapMarkerDto::new));
    }



}