package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MapMarkerDto;
import ge.dola.talanti.map.dto.SaveLocationDto;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Records;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static ge.dola.talanti.jooq.Tables.CLUBS;
import static ge.dola.talanti.jooq.Tables.CLUB_FOLLOWS;
import static ge.dola.talanti.jooq.Tables.CLUB_MEMBERSHIPS;
import static ge.dola.talanti.jooq.Tables.LOCATIONS;
import static ge.dola.talanti.jooq.Tables.MATCHES;
import static ge.dola.talanti.jooq.Tables.TRYOUTS;

@Repository
public class MapRepository {

    private final DSLContext dsl;

    public MapRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Long saveLocation(SaveLocationDto dto) {
        return dsl.insertInto(LOCATIONS)
                .set(LOCATIONS.LATITUDE, BigDecimal.valueOf(dto.latitude()))
                .set(LOCATIONS.LONGITUDE, BigDecimal.valueOf(dto.longitude()))
                .set(LOCATIONS.ADDRESS_TEXT, dto.addressText() == null || dto.addressText().isBlank() ? null : dto.addressText().trim())
                .set(LOCATIONS.CREATED_AT, LocalDateTime.now())
                .returningResult(LOCATIONS.ID)
                .fetchOneInto(Long.class);
    }

    private Condition buildLocationCondition(Condition base, List<String> cities, List<String> countries) {
        Condition updated = base;

        if (cities != null && !cities.isEmpty()) {
            Condition cityCondition = DSL.falseCondition();
            for (String city : cities) {
                cityCondition = cityCondition.or(LOCATIONS.ADDRESS_TEXT.containsIgnoreCase(city));
            }
            updated = updated.and(cityCondition);
        }

        if (countries != null && !countries.isEmpty()) {
            Condition countryCondition = DSL.falseCondition();
            for (String country : countries) {
                countryCondition = countryCondition.or(LOCATIONS.ADDRESS_TEXT.containsIgnoreCase(country));
            }
            updated = updated.and(countryCondition);
        }

        return updated;
    }

    @SafeVarargs
    private final Condition buildSearchCondition(Condition base, String query, Field<String>... fields) {
        if (query == null || query.isBlank()) {
            return base;
        }

        Condition searchCondition = DSL.falseCondition();
        for (Field<String> field : fields) {
            searchCondition = searchCondition.or(DSL.coalesce(field, DSL.inline("")).containsIgnoreCase(query));
        }

        return base.and(searchCondition);
    }

    private Field<Double> getHaversineFormula(Double lat, Double lng) {
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

    public List<MapMarkerDto> findNearbyClubs(Double lat, Double lng, Double radiusKm, List<String> cities, List<String> countries, String query) {
        Field<Double> distanceMath = getHaversineFormula(lat, lng);
        Field<Double> distanceKm = distanceMath.as("distanceKm");

        Condition conditions = buildLocationCondition(distanceMath.le(radiusKm), cities, countries);
        conditions = buildSearchCondition(conditions, query, CLUBS.NAME, CLUBS.TYPE, LOCATIONS.ADDRESS_TEXT);

                return dsl.select(
                        CLUBS.ID.as("entityId"),
                        DSL.inline("CLUB").as("entityType"),
                        CLUBS.NAME.as("title"),
                        CLUBS.TYPE.as("subtitle"),
                        CLUBS.NAME.as("clubName"),
                        LOCATIONS.LATITUDE.cast(Double.class).as("latitude"),
                        LOCATIONS.LONGITUDE.cast(Double.class).as("longitude"),
                        distanceKm,
                        DSL.field(DSL.selectCount().from(CLUB_MEMBERSHIPS).where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))).as("members"),
                        DSL.field(DSL.selectCount().from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID))).as("followers"),
                        CLUBS.STATUS.eq("VERIFIED").as("verified"),
                        DSL.inline("").as("date"),
                        DSL.inline("").as("fee"),
                        LOCATIONS.ADDRESS_TEXT.as("addressText"),
                        DSL.inline("").as("ageGroup"),
                        CLUBS.STATUS.as("status")
                )
                .from(CLUBS)
                .join(LOCATIONS).on(CLUBS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(conditions)
                .orderBy(distanceMath.asc(), CLUBS.NAME.asc())
                .fetch(Records.mapping(MapMarkerDto::new));
    }

    public List<MapMarkerDto> findNearbyTryouts(Double lat, Double lng, Double radiusKm, List<String> gender, List<String> ageGroups, List<String> cities, List<String> countries, String query) {
        var hostClub = CLUBS.as("hostClub");
        Field<Double> distanceMath = getHaversineFormula(lat, lng);
        Field<Double> distanceKm = distanceMath.as("distanceKm");
        Field<String> tryoutSubtitle = DSL.when(
                        TRYOUTS.POSITION.isNotNull().and(TRYOUTS.POSITION.ne("")),
                        DSL.concat(hostClub.NAME, DSL.inline(" - "), TRYOUTS.POSITION)
                )
                .otherwise(hostClub.NAME)
                .as("subtitle");

        Condition conditions = buildLocationCondition(distanceMath.le(radiusKm), cities, countries);
        conditions = conditions.and(TRYOUTS.TRYOUT_DATE.greaterOrEqual(LocalDateTime.now()));
        conditions = buildSearchCondition(conditions, query, TRYOUTS.TITLE, hostClub.NAME, TRYOUTS.POSITION, LOCATIONS.ADDRESS_TEXT);

        if (ageGroups != null && !ageGroups.isEmpty()) {
            conditions = conditions.and(TRYOUTS.AGE_GROUP.in(ageGroups));
        }

                return dsl.select(
                        TRYOUTS.ID.as("entityId"),
                        DSL.inline("TRYOUT").as("entityType"),
                        TRYOUTS.TITLE.as("title"),
                        tryoutSubtitle,
                        hostClub.NAME.as("clubName"),
                        LOCATIONS.LATITUDE.cast(Double.class).as("latitude"),
                        LOCATIONS.LONGITUDE.cast(Double.class).as("longitude"),
                        distanceKm,
                        DSL.inline(0).as("members"),
                        DSL.inline(0).as("followers"),
                        DSL.inline(false).as("verified"),
                        DSL.cast(TRYOUTS.TRYOUT_DATE, String.class).as("date"),
                        DSL.inline("Free").as("fee"),
                        LOCATIONS.ADDRESS_TEXT.as("addressText"),
                        TRYOUTS.AGE_GROUP.as("ageGroup"),
                        DSL.inline("OPEN").as("status")
                )
                .from(TRYOUTS)
                .join(hostClub).on(TRYOUTS.CLUB_ID.eq(hostClub.ID))
                .join(LOCATIONS).on(TRYOUTS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(conditions)
                .orderBy(distanceMath.asc(), TRYOUTS.TRYOUT_DATE.asc())
                .fetch(Records.mapping(MapMarkerDto::new));
    }

    public List<MapMarkerDto> findNearbyMatches(Double lat, Double lng, Double radiusKm, List<String> gender, List<String> ageGroups, List<String> cities, List<String> countries, String query, String requestedEntityType) {
        var homeClub = CLUBS.as("homeClub");
        var awayClub = CLUBS.as("awayClub");
        Field<Double> distanceMath = getHaversineFormula(lat, lng);
        Field<Double> distanceKm = distanceMath.as("distanceKm");
        Field<String> title = DSL.concat(homeClub.NAME, DSL.inline(" vs "), DSL.coalesce(awayClub.NAME, DSL.inline("TBD"))).as("title");
        Field<String> subtitle = DSL.concat(MATCHES.MATCH_TYPE, DSL.inline(" - "), MATCHES.STATUS).as("subtitle");

        Condition conditions = buildLocationCondition(distanceMath.le(radiusKm), cities, countries);
        conditions = conditions.and(MATCHES.SCHEDULED_DATE.greaterOrEqual(LocalDateTime.now()));
        conditions = conditions.and(MATCHES.STATUS.eq("OPEN"));
        if ("FRIENDLY".equals(requestedEntityType)) {
            conditions = conditions.and(MATCHES.MATCH_TYPE.eq("FRIENDLY"));
        } else {
            conditions = conditions.and(MATCHES.MATCH_TYPE.eq("COMPETITIVE"));
        }
        conditions = buildSearchCondition(conditions, query, homeClub.NAME, awayClub.NAME, MATCHES.MATCH_TYPE, LOCATIONS.ADDRESS_TEXT);

                return dsl.select(
                        MATCHES.ID.as("entityId"),
                        DSL.inline(requestedEntityType).as("entityType"),
                        title,
                        subtitle,
                        homeClub.NAME.as("clubName"),
                        LOCATIONS.LATITUDE.cast(Double.class).as("latitude"),
                        LOCATIONS.LONGITUDE.cast(Double.class).as("longitude"),
                        distanceKm,
                        DSL.inline(0).as("members"),
                        DSL.inline(0).as("followers"),
                        DSL.inline(false).as("verified"),
                        DSL.cast(MATCHES.SCHEDULED_DATE, String.class).as("date"),
                        DSL.inline("Free").as("fee"),
                        LOCATIONS.ADDRESS_TEXT.as("addressText"),
                        DSL.inline("").as("ageGroup"),
                        MATCHES.STATUS.as("status")
                )
                .from(MATCHES)
                .join(homeClub).on(MATCHES.HOME_CLUB_ID.eq(homeClub.ID))
                .leftJoin(awayClub).on(MATCHES.AWAY_CLUB_ID.eq(awayClub.ID))
                .join(LOCATIONS).on(LOCATIONS.ID.eq(DSL.coalesce(MATCHES.LOCATION_ID, homeClub.LOCATION_ID)))
                .where(conditions)
                .orderBy(distanceMath.asc(), MATCHES.SCHEDULED_DATE.asc())
                .fetch(Records.mapping(MapMarkerDto::new));
    }
}
