package ge.dola.talanti.club;


import ge.dola.talanti.config.ResourceNotFoundException;
import ge.dola.talanti.club.dto.*;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.*;

@Repository
@RequiredArgsConstructor
public class ClubProfileRepository {

    private final DSLContext dsl;

    public Optional<ClubProfileDto> getClubProfile(Long clubId, Long currentUserId) {
        // 1. Fetch the main club record
        var record = dsl.select(
                        CLUBS.ID,
                        CLUBS.NAME,
                        CLUBS.DESCRIPTION,
                        CLUBS.TYPE,
                        CLUBS.STATUS,
                        DSL.field(DSL.selectCount().from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID))).as("followerCount"),
                        DSL.field(DSL.selectCount().from(CLUB_MEMBERSHIPS).where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))).as("memberCount"),
                        DSL.field(DSL.exists(DSL.selectOne().from(CLUB_FOLLOWS)
                                .where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID))
                                .and(CLUB_FOLLOWS.USER_ID.eq(currentUserId)))).as("isFollowedByMe"),
                        DSL.field(DSL.exists(DSL.selectOne().from(CLUB_MEMBERSHIPS)
                                .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))
                                .and(CLUB_MEMBERSHIPS.USER_ID.eq(currentUserId)))).as("isMember"),
                        DSL.field(DSL.select(CLUB_MEMBERSHIPS.ROLE).from(CLUB_MEMBERSHIPS)
                                .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))
                                .and(CLUB_MEMBERSHIPS.USER_ID.eq(currentUserId))).as("myRole"),
                        LOCATIONS.ADDRESS_TEXT,
                        LOCATIONS.LATITUDE,
                        LOCATIONS.LONGITUDE,
                        CLUBS.LOGO_URL,
                        CLUBS.BANNER_URL,
                        CLUBS.WHATSAPP_NUMBER,
                        ClubDynamicTables.CLUBS_FACEBOOK_MESSENGER_URL,
                        ClubDynamicTables.CLUBS_PREFERRED_CONTACT_METHOD
                )
                .from(CLUBS)
                .leftJoin(LOCATIONS).on(CLUBS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(CLUBS.ID.eq(clubId))
                .fetchOptional();

        if (record.isEmpty()) {
            return Optional.empty();
        }

        // 2. Fetch the dynamic list of opportunities
        List<ClubOpportunityDto> opportunities = dsl.select(
                        CLUB_OPPORTUNITIES.ID,
                        CLUB_OPPORTUNITIES.TYPE,
                        CLUB_OPPORTUNITIES.TITLE,
                        CLUB_OPPORTUNITIES.EXTERNAL_LINK
                )
                .from(CLUB_OPPORTUNITIES)
                .where(CLUB_OPPORTUNITIES.CLUB_ID.eq(clubId))
                .and(CLUB_OPPORTUNITIES.STATUS.eq("OPEN"))
                .orderBy(CLUB_OPPORTUNITIES.CREATED_AT.desc(), CLUB_OPPORTUNITIES.ID.desc())
                .fetchInto(ClubOpportunityDto.class);

        List<ClubHonourDto> honours = dsl.select(
                        CLUB_HONOURS.ID,
                        CLUB_HONOURS.TITLE,
                        CLUB_HONOURS.YEAR_WON,
                        CLUB_HONOURS.DESCRIPTION
                )
                .from(CLUB_HONOURS)
                .where(CLUB_HONOURS.CLUB_ID.eq(clubId))
                .orderBy(CLUB_HONOURS.YEAR_WON.desc(), CLUB_HONOURS.ID.desc())
                .fetchInto(ClubHonourDto.class);

        List<ClubTrustReferenceDto> trustedByClubs = dsl.select(
                        CLUBS.ID.as("clubId"),
                        CLUBS.NAME.as("clubName")
                )
                .from(ClubDynamicTables.CLUB_TRUST_LINKS)
                .join(CLUBS).on(ClubDynamicTables.CLUB_TRUST_LINKS_TRUSTING_CLUB_ID.eq(CLUBS.ID))
                .where(ClubDynamicTables.CLUB_TRUST_LINKS_TRUSTED_CLUB_ID.eq(clubId))
                .orderBy(
                        DSL.when(CLUBS.STATUS.eq("VERIFIED"), 0).otherwise(1).asc(),
                        CLUBS.NAME.asc(),
                        ClubDynamicTables.CLUB_TRUST_LINKS_CREATED_AT.desc()
                )
                .limit(3)
                .fetchInto(ClubTrustReferenceDto.class);

        // 3. Construct the Record safely
        var r = record.get();
        return Optional.of(new ClubProfileDto(
                r.get(CLUBS.ID),
                r.get(CLUBS.NAME),
                r.get(CLUBS.DESCRIPTION),
                r.get(CLUBS.TYPE),
                "VERIFIED".equals(r.get(CLUBS.STATUS)),
                toStatusLabel(r.get(CLUBS.STATUS)),
                r.get("followerCount", Integer.class) == null ? 0 : r.get("followerCount", Integer.class),
                r.get("memberCount", Integer.class) == null ? 0 : r.get("memberCount", Integer.class),
                Boolean.TRUE.equals(r.get("isFollowedByMe", Boolean.class)),
                Boolean.TRUE.equals(r.get("isMember", Boolean.class)),
                r.get("myRole", String.class),
                r.get(LOCATIONS.ADDRESS_TEXT),
                r.get(CLUBS.LOGO_URL),
                r.get(CLUBS.BANNER_URL),
                r.get(CLUBS.WHATSAPP_NUMBER),
                r.get(ClubDynamicTables.CLUBS_FACEBOOK_MESSENGER_URL),
                r.get(ClubDynamicTables.CLUBS_PREFERRED_CONTACT_METHOD),
                r.get(LOCATIONS.LATITUDE) != null ? r.get(LOCATIONS.LATITUDE).doubleValue() : null,
                r.get(LOCATIONS.LONGITUDE) != null ? r.get(LOCATIONS.LONGITUDE).doubleValue() : null,
                trustedByClubs,
                honours,
                opportunities // Inject the fetched list!
        ));
    }

    public Long issueMatchChallenge(Long sourceClubId, Long targetClubId, CreateChallengeDto dto) {
        // Enforce the schema requirement for matches
        return dsl.insertInto(MATCHES)
                .set(MATCHES.HOME_CLUB_ID, targetClubId) // The challenged club hosts by default logically
                .set(MATCHES.AWAY_CLUB_ID, sourceClubId)
                .set(MATCHES.MATCH_TYPE, dto.matchType() != null ? dto.matchType().trim().toUpperCase() : "FRIENDLY")
                .set(MATCHES.STATUS, "PENDING_ACCEPTANCE")
                .set(MATCHES.SCHEDULED_DATE, dto.proposedDate())
                .set(MATCHES.CREATED_AT, LocalDateTime.now())
                .returningResult(MATCHES.ID)
                .fetchOneInto(Long.class);
    }

    public void createCalendarEvent(Long clubId, Long userId, CalendarRequestDto request) {
        LocalDateTime requestedDate = request.date();
        Long targetLocId = resolveClubLocationId(clubId, request.targetLocationClubId(), "TRYOUT".equals(request.type()));

        if ("TRYOUT".equals(request.type())) {
            dsl.insertInto(TRYOUTS)
                    .set(TRYOUTS.CLUB_ID, clubId)
                    .set(TRYOUTS.TITLE, request.title())
                    .set(TRYOUTS.DESCRIPTION, "Open calendar event")
                    .set(TRYOUTS.AGE_GROUP, request.ageGroup() != null ? request.ageGroup() : "OPEN")
                    .set(TRYOUTS.LOCATION_ID, targetLocId)
                    .set(TRYOUTS.TRYOUT_DATE, requestedDate)
                    .set(TRYOUTS.CREATED_BY, userId)
                    .execute();
        } else if ("AVAILABILITY".equals(request.type())) {
            List<String> notes = new ArrayList<>();
            if (request.gender() != null && !request.gender().isBlank()) {
                notes.add("Gender: " + request.gender().trim().toUpperCase());
            }
            if (request.willingToTravel() != null) {
                notes.add("Willing to travel: " + request.willingToTravel());
            }

            String summary = notes.isEmpty()
                    ? "Looking for matches."
                    : "Looking for matches. " + String.join(". ", notes) + ".";

            dsl.insertInto(CLUB_SCHEDULES)
                    .set(CLUB_SCHEDULES.CLUB_ID, clubId)
                    .set(CLUB_SCHEDULES.DATE, requestedDate.toLocalDate())
                    .set(CLUB_SCHEDULES.STATUS, "FREE")
                    .set(CLUB_SCHEDULES.NOTES, summary)
                    .onDuplicateKeyUpdate()
                    .set(CLUB_SCHEDULES.NOTES, summary)
                    .execute();
        } else {
            throw new IllegalArgumentException("Unsupported calendar event type: " + request.type());
        }
    }

    public List<ClubProfileDto> getAllClubs(Long currentUserId) {
        var records = dsl.select(
                        CLUBS.ID,
                        CLUBS.NAME,
                        CLUBS.DESCRIPTION,
                        CLUBS.TYPE,
                        CLUBS.STATUS,
                        DSL.field(DSL.selectCount().from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID))).as("followerCount"),
                        DSL.field(DSL.selectCount().from(CLUB_MEMBERSHIPS).where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))).as("memberCount"),
                        currentUserId == null
                                ? DSL.inline(false).as("isFollowedByMe")
                                : DSL.field(DSL.exists(
                                DSL.selectOne().from(CLUB_FOLLOWS)
                                        .where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID))
                                        .and(CLUB_FOLLOWS.USER_ID.eq(currentUserId))
                        )).as("isFollowedByMe"),
                        currentUserId == null
                                ? DSL.inline(false).as("isMember")
                                : DSL.field(DSL.exists(
                                DSL.selectOne().from(CLUB_MEMBERSHIPS)
                                        .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))
                                        .and(CLUB_MEMBERSHIPS.USER_ID.eq(currentUserId))
                        )).as("isMember"),
                        currentUserId == null
                                ? DSL.inline((String) null).as("myRole")
                                : DSL.field(
                                DSL.select(CLUB_MEMBERSHIPS.ROLE)
                                        .from(CLUB_MEMBERSHIPS)
                                        .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))
                                        .and(CLUB_MEMBERSHIPS.USER_ID.eq(currentUserId))
                        ).as("myRole"),
                        LOCATIONS.ADDRESS_TEXT,
                        LOCATIONS.LATITUDE,
                        LOCATIONS.LONGITUDE,
                        CLUBS.LOGO_URL,
                        CLUBS.BANNER_URL,
                        CLUBS.WHATSAPP_NUMBER,
                        ClubDynamicTables.CLUBS_FACEBOOK_MESSENGER_URL,
                        ClubDynamicTables.CLUBS_PREFERRED_CONTACT_METHOD
                )
                .from(CLUBS)
                .leftJoin(LOCATIONS).on(CLUBS.LOCATION_ID.eq(LOCATIONS.ID))
                .orderBy(CLUBS.CREATED_AT.desc(), CLUBS.ID.desc())
                .fetch();

        if (records.isEmpty()) {
            return List.of();
        }

        List<Long> clubIds = records.map(r -> r.get(CLUBS.ID));

        Map<Long, List<ClubOpportunityDto>> opportunitiesByClub = dsl.select(
                        CLUB_OPPORTUNITIES.CLUB_ID,
                        CLUB_OPPORTUNITIES.ID,
                        CLUB_OPPORTUNITIES.TYPE,
                        CLUB_OPPORTUNITIES.TITLE,
                        CLUB_OPPORTUNITIES.EXTERNAL_LINK
                )
                .from(CLUB_OPPORTUNITIES)
                .where(CLUB_OPPORTUNITIES.CLUB_ID.in(clubIds))
                .and(CLUB_OPPORTUNITIES.STATUS.eq("OPEN"))
                .orderBy(CLUB_OPPORTUNITIES.CLUB_ID.asc(), CLUB_OPPORTUNITIES.CREATED_AT.desc(), CLUB_OPPORTUNITIES.ID.desc())
                .fetchGroups(
                        CLUB_OPPORTUNITIES.CLUB_ID,
                        record -> new ClubOpportunityDto(
                                record.get(CLUB_OPPORTUNITIES.ID),
                                record.get(CLUB_OPPORTUNITIES.TYPE),
                                record.get(CLUB_OPPORTUNITIES.TITLE),
                                record.get(CLUB_OPPORTUNITIES.EXTERNAL_LINK)
                        )
                );

        return records.map(r -> new ClubProfileDto(
                r.get(CLUBS.ID),
                r.get(CLUBS.NAME),
                r.get(CLUBS.DESCRIPTION),
                r.get(CLUBS.TYPE),
                "VERIFIED".equals(r.get(CLUBS.STATUS)),
                toStatusLabel(r.get(CLUBS.STATUS)),
                r.get("followerCount", Integer.class) == null ? 0 : r.get("followerCount", Integer.class),
                r.get("memberCount", Integer.class) == null ? 0 : r.get("memberCount", Integer.class),
                Boolean.TRUE.equals(r.get("isFollowedByMe", Boolean.class)),
                Boolean.TRUE.equals(r.get("isMember", Boolean.class)),
                r.get("myRole", String.class),
                r.get(LOCATIONS.ADDRESS_TEXT),
                r.get(CLUBS.LOGO_URL),
                r.get(CLUBS.BANNER_URL),
                r.get(CLUBS.WHATSAPP_NUMBER),
                r.get(ClubDynamicTables.CLUBS_FACEBOOK_MESSENGER_URL),
                r.get(ClubDynamicTables.CLUBS_PREFERRED_CONTACT_METHOD),
                r.get(LOCATIONS.LATITUDE) != null ? r.get(LOCATIONS.LATITUDE).doubleValue() : null,
                r.get(LOCATIONS.LONGITUDE) != null ? r.get(LOCATIONS.LONGITUDE).doubleValue() : null,
                List.of(),
                List.of(),
                opportunitiesByClub.getOrDefault(r.get(CLUBS.ID), List.of())
        ));
    }

    // 1. Get the Club Roster (Populates the "Teams/Roster" Tab)
    public List<ClubRosterDto> getClubRoster(Long clubId) {
        return dsl.select(
                        USERS.ID,
                        USER_PROFILES.FULL_NAME.as("name"),
                        PLAYER_DETAILS.PRIMARY_POSITION.as("position"),
                        SQUAD_PLAYERS.JERSEY_NUMBER.as("number"),
                        PLAYER_DETAILS.AVAILABILITY_STATUS.as("status"),
                        USER_PROFILES.PROFILE_PICTURE_URL.as("avatar"),
                        SQUADS.ID.as("squadId"),
                        SQUADS.NAME.as("squadName"),
                        SQUADS.CATEGORY.as("squadCategory"),
                        SQUADS.GENDER.as("squadGender")
                )
                .from(SQUAD_PLAYERS)
                .join(SQUADS).on(SQUAD_PLAYERS.SQUAD_ID.eq(SQUADS.ID))
                .join(USERS).on(SQUAD_PLAYERS.USER_ID.eq(USERS.ID))
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .leftJoin(PLAYER_DETAILS).on(USERS.ID.eq(PLAYER_DETAILS.USER_ID))
                .where(SQUADS.CLUB_ID.eq(clubId))
                .orderBy(SQUADS.CATEGORY.asc(), SQUADS.NAME.asc(), SQUAD_PLAYERS.JERSEY_NUMBER.asc(), USER_PROFILES.FULL_NAME.asc())
                .fetchInto(ClubRosterDto.class);
    }

    // 2. Get the Club Staff (Populates the Admin/Staff section)
    public List<ClubStaffDto> getClubStaff(Long clubId) {
        return dsl.select(
                        USERS.ID,
                        USER_PROFILES.FULL_NAME.as("name"),
                        CLUB_MEMBERSHIPS.ROLE.as("role"),
                        DSL.val("VERIFIED").as("clearance") // Hardcoded for MVP, adjust later
                )
                .from(CLUB_MEMBERSHIPS)
                .join(USERS).on(CLUB_MEMBERSHIPS.USER_ID.eq(USERS.ID))
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(clubId))
                .and(CLUB_MEMBERSHIPS.ROLE.in("OWNER", "CLUB_ADMIN", "COACH", "AGENT"))
                .orderBy(
                        DSL.when(CLUB_MEMBERSHIPS.ROLE.eq("OWNER"), 0)
                                .when(CLUB_MEMBERSHIPS.ROLE.eq("CLUB_ADMIN"), 1)
                                .when(CLUB_MEMBERSHIPS.ROLE.eq("COACH"), 2)
                                .otherwise(3)
                                .asc(),
                        USER_PROFILES.FULL_NAME.asc().nullsLast(),
                        USERS.ID.asc()
                )
                .fetchInto(ClubStaffDto.class);
    }

    // 3. Get the Club Schedule (Populates the Calendar Tab)
    public List<CalendarEventDto> getClubSchedule(Long clubId) {
        var homeClub = CLUBS.as("homeClub");
        var awayClub = CLUBS.as("awayClub");

        // Union query: Combines matches, tryouts, and open availability into a single calendar feed.
        var matches = dsl.select(
                        DSL.concat(DSL.val("MATCH:"), MATCHES.ID.cast(String.class)).as("id"),
                        DSL.val("MATCH").as("type"),
                        DSL.concat(
                                DSL.val("Match vs "),
                                DSL.when(MATCHES.HOME_CLUB_ID.eq(clubId), awayClub.NAME).otherwise(homeClub.NAME)
                        ).as("title"),
                        MATCHES.SCHEDULED_DATE.cast(String.class).as("date"),
                        LOCATIONS.ADDRESS_TEXT.as("location"),
                        MATCHES.STATUS.as("status")
                )
                .from(MATCHES)
                .leftJoin(homeClub).on(MATCHES.HOME_CLUB_ID.eq(homeClub.ID))
                .leftJoin(awayClub).on(MATCHES.AWAY_CLUB_ID.eq(awayClub.ID))
                .leftJoin(LOCATIONS).on(MATCHES.LOCATION_ID.eq(LOCATIONS.ID))
                .where(MATCHES.HOME_CLUB_ID.eq(clubId).or(MATCHES.AWAY_CLUB_ID.eq(clubId)));

        var tryouts = dsl.select(
                        DSL.concat(DSL.val("TRYOUT:"), TRYOUTS.ID.cast(String.class)).as("id"),
                        DSL.val("TRYOUT").as("type"),
                        TRYOUTS.TITLE.as("title"),
                        TRYOUTS.TRYOUT_DATE.cast(String.class).as("date"),
                        LOCATIONS.ADDRESS_TEXT.as("location"),
                        DSL.val("UPCOMING").as("status")
                )
                .from(TRYOUTS)
                .leftJoin(LOCATIONS).on(TRYOUTS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(TRYOUTS.CLUB_ID.eq(clubId));

        var availability = dsl.select(
                        DSL.concat(DSL.val("AVAILABILITY:"), CLUB_SCHEDULES.ID.cast(String.class)).as("id"),
                        DSL.val("AVAILABILITY").as("type"),
                        DSL.val("Open Match Availability").as("title"),
                        CLUB_SCHEDULES.DATE.cast(String.class).as("date"),
                        LOCATIONS.ADDRESS_TEXT.as("location"),
                        CLUB_SCHEDULES.STATUS.as("status")
                )
                .from(CLUB_SCHEDULES)
                .join(CLUBS).on(CLUB_SCHEDULES.CLUB_ID.eq(CLUBS.ID))
                .leftJoin(LOCATIONS).on(CLUBS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(CLUB_SCHEDULES.CLUB_ID.eq(clubId));

        return matches.unionAll(tryouts).unionAll(availability)
                .fetchInto(CalendarEventDto.class)
                .stream()
                .sorted(
                        Comparator.comparing(ClubProfileRepository::parseEventDate, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(CalendarEventDto::type)
                                .thenComparing(CalendarEventDto::id)
                )
                .toList();
    }

    // 4. Find My Primary Club (For the "My Club" navigation button)
    public Optional<MyClubResponseDto> getMyPrimaryClub(Long userId) {
        return dsl.select(
                        CLUBS.ID.as("clubId"),
                        CLUBS.NAME.as("clubName"),
                        CLUB_MEMBERSHIPS.ROLE.as("myRole")
                )
                .from(CLUB_MEMBERSHIPS)
                .join(CLUBS).on(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))
                .where(CLUB_MEMBERSHIPS.USER_ID.eq(userId))
                .and(CLUB_MEMBERSHIPS.ROLE.in("OWNER", "CLUB_ADMIN"))
                .orderBy(
                        DSL.when(CLUB_MEMBERSHIPS.ROLE.eq("OWNER"), 0)
                                .when(CLUB_MEMBERSHIPS.ROLE.eq("CLUB_ADMIN"), 1)
                                .otherwise(2)
                                .asc(),
                        CLUB_MEMBERSHIPS.JOINED_AT.asc().nullsLast(),
                        CLUBS.CREATED_AT.asc()
                )
                .limit(1)
                .fetchOptionalInto(MyClubResponseDto.class);
    }


    public void deleteCalendarEvent(Long clubId, String eventId) {
        String eventType = null;
        String rawId = eventId;

        int separatorIndex = eventId.indexOf(':');
        if (separatorIndex > 0) {
            eventType = eventId.substring(0, separatorIndex).toUpperCase();
            rawId = eventId.substring(separatorIndex + 1);
        }

        Long parsedEventId;
        try {
            parsedEventId = Long.parseLong(rawId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid event identifier.");
        }

        int deleted = 0;
        if (eventType == null || "TRYOUT".equals(eventType)) {
            deleted += dsl.deleteFrom(TRYOUTS)
                    .where(TRYOUTS.ID.eq(parsedEventId))
                    .and(TRYOUTS.CLUB_ID.eq(clubId))
                    .execute();
        }

        if (eventType == null || "AVAILABILITY".equals(eventType) || "CLUB_SCHEDULE".equals(eventType)) {
            deleted += dsl.deleteFrom(CLUB_SCHEDULES)
                    .where(CLUB_SCHEDULES.ID.eq(parsedEventId))
                    .and(CLUB_SCHEDULES.CLUB_ID.eq(clubId))
                    .execute();
        }

        if (eventType == null || "MATCH".equals(eventType)) {
            deleted += dsl.deleteFrom(MATCHES)
                    .where(MATCHES.ID.eq(parsedEventId))
                    .and(MATCHES.HOME_CLUB_ID.eq(clubId).or(MATCHES.AWAY_CLUB_ID.eq(clubId)))
                    .execute();
        }

        if (deleted == 0) {
            throw new IllegalArgumentException("Event not found or does not belong to this club.");
        }
    }

    private Long resolveClubLocationId(Long owningClubId, Long requestedLocationClubId, boolean locationRequired) {
        Long sourceClubId = requestedLocationClubId != null ? requestedLocationClubId : owningClubId;

        var clubRecord = dsl.select(CLUBS.ID, CLUBS.LOCATION_ID)
                .from(CLUBS)
                .where(CLUBS.ID.eq(sourceClubId))
                .fetchOptional()
                .orElseThrow(() -> new ResourceNotFoundException("Club not found."));

        Long locationId = clubRecord.get(CLUBS.LOCATION_ID);
        if (locationRequired && locationId == null) {
            throw new IllegalArgumentException("A saved club location is required for this event.");
        }

        return locationId;
    }

    private static LocalDateTime parseEventDate(CalendarEventDto event) {
        if (event == null || event.date() == null || event.date().isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(event.date());
        } catch (Exception ignored) {
            try {
                return LocalDate.parse(event.date()).atStartOfDay();
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    private static String toStatusLabel(String status) {
        if ("VERIFIED".equalsIgnoreCase(status)) {
            return "Verified";
        }
        if ("NEW_CLUB".equalsIgnoreCase(status)) {
            return "New club";
        }
        return "Unverified";
    }
}
