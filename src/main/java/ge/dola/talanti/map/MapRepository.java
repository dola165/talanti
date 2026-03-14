package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MapMarkerDto;
import ge.dola.talanti.map.dto.SaveLocationDto;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static ge.dola.talanti.jooq.Tables.*;

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

    // --- SHARED HELPER FOR LOCATION STRINGS ---
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

    // --- CLUBS ---
    public List<MapMarkerDto> findNearbyClubs(Double lat, Double lng, Double radiusKm, List<String> cities, List<String> countries) {
        Field<Double> distanceKm = getHaversineFormula(lat, lng);
        Condition conditions = buildLocationCondition(distanceKm.le(radiusKm), cities, countries);

        return dsl.select(
                        CLUBS.ID.as("entityId"),
                        DSL.inline("CLUB").as("entityType"),
                        CLUBS.NAME.as("title"),
                        CLUBS.TYPE.as("subtitle"),
                        LOCATIONS.LATITUDE,
                        LOCATIONS.LONGITUDE,
                        distanceKm.as("distanceKm"),

                        DSL.field(DSL.selectCount().from(CLUB_MEMBERSHIPS).where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))).as("members"),
                        DSL.field(DSL.selectCount().from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID))).as("followers"),
                        CLUBS.IS_OFFICIAL.as("verified"),
                        DSL.inline("").as("date"),
                        DSL.inline("").as("fee")
                )
                .from(CLUBS)
                .join(LOCATIONS).on(CLUBS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(conditions)
                .fetchInto(MapMarkerDto.class);
    }

    // --- TRYOUTS ---
    public List<MapMarkerDto> findNearbyTryouts(Double lat, Double lng, Double radiusKm, List<String> gender, List<String> ageGroups, List<String> cities, List<String> countries) {
        Field<Double> distanceKm = getHaversineFormula(lat, lng);
        Condition conditions = buildLocationCondition(distanceKm.le(radiusKm), cities, countries);
        conditions = conditions.and(TRYOUTS.TRYOUT_DATE.greaterOrEqual(LocalDateTime.now()));

        if (ageGroups != null && !ageGroups.isEmpty()) {
            conditions = conditions.and(TRYOUTS.AGE_GROUP.in(ageGroups));
        }

        return dsl.select(
                        TRYOUTS.ID.as("entityId"),
                        DSL.inline("TRYOUT").as("entityType"),
                        TRYOUTS.TITLE.as("title"),
                        TRYOUTS.POSITION.as("subtitle"),
                        LOCATIONS.LATITUDE,
                        LOCATIONS.LONGITUDE,
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
                .fetchInto(MapMarkerDto.class);
    }

    // --- MATCHES ---
    public List<MapMarkerDto> findNearbyMatches(Double lat, Double lng, Double radiusKm, List<String> gender, List<String> ageGroups, List<String> cities, List<String> countries) {
        Field<Double> distanceKm = getHaversineFormula(lat, lng);
        Condition conditions = buildLocationCondition(distanceKm.le(radiusKm), cities, countries);
        conditions = conditions.and(MATCH_REQUESTS.DESIRED_DATE.greaterOrEqual(LocalDate.now().atStartOfDay()));

        if (gender != null && !gender.isEmpty()) {
            // Note: Gender strings match "BOYS", "GIRLS" from frontend, map to "MALE", "FEMALE" if your DB requires!
            // Assuming DB matches the array values directly for now.
            conditions = conditions.and(SQUADS.GENDER.in(gender));
        }
        if (ageGroups != null && !ageGroups.isEmpty()) {
            conditions = conditions.and(SQUADS.CATEGORY.in(ageGroups));
        }

        return dsl.select(
                        MATCH_REQUESTS.ID.as("entityId"),
                        DSL.inline("MATCH_REQUEST").as("entityType"),
                        DSL.concat(CLUBS.NAME, DSL.inline(" - "), SQUADS.NAME).as("title"),
                        MATCH_REQUESTS.STATUS.as("subtitle"),
                        LOCATIONS.LATITUDE,
                        LOCATIONS.LONGITUDE,
                        distanceKm.as("distanceKm"),
                        DSL.inline(0).as("members"),
                        DSL.inline(0).as("followers"),
                        DSL.inline(false).as("verified"),
                        DSL.cast(MATCH_REQUESTS.DESIRED_DATE, String.class).as("date"),
                        DSL.inline("Free").as("fee")
                )
                .from(MATCH_REQUESTS)
                .join(CLUBS).on(MATCH_REQUESTS.CLUB_ID.eq(CLUBS.ID))
                .join(SQUADS).on(MATCH_REQUESTS.SQUAD_ID.eq(SQUADS.ID))
                .join(LOCATIONS).on(MATCH_REQUESTS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(conditions)
                .fetchInto(MapMarkerDto.class);
    }

    private Field<Double> getHaversineFormula(Double lat, Double lng) {
        return DSL.field(
                "6371 * acos(cos(radians({0})) * cos(radians(" + LOCATIONS.LATITUDE.getName() + ")) * " +
                        "cos(radians(" + LOCATIONS.LONGITUDE.getName() + ") - radians({1})) + " +
                        "sin(radians({0})) * sin(radians(" + LOCATIONS.LATITUDE.getName() + ")))",
                Double.class,
                lat, lng
        );
    }
}