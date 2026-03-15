package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.*;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.*;

@Repository
@RequiredArgsConstructor
public class ClubProfileRepository {

    private final DSLContext dsl;

    public Optional<ClubProfileDto> getClubProfile(Long clubId, Long currentUserId) {
        return dsl.select(
                        CLUBS.ID.as("id"),
                        CLUBS.NAME.as("name"),
                        CLUBS.DESCRIPTION.as("description"),
                        CLUBS.TYPE.as("type"),
                        CLUBS.IS_OFFICIAL.as("isOfficial"),

                        // Counts come next in the Record
                        DSL.field(DSL.selectCount().from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID))).as("followerCount"),
                        DSL.field(DSL.selectCount().from(CLUB_MEMBERSHIPS).where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))).as("memberCount"),

                        // Booleans
                        DSL.field(DSL.exists(DSL.selectOne().from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID).and(CLUB_FOLLOWS.USER_ID.eq(currentUserId))))).as("isFollowedByMe"),
                        DSL.field(DSL.exists(DSL.selectOne().from(CLUB_MEMBERSHIPS).where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID).and(CLUB_MEMBERSHIPS.USER_ID.eq(currentUserId))))).as("isMember"),

                        // Strings at the end
                        LOCATIONS.ADDRESS_TEXT.as("addressText"),
                        CLUBS.LOGO_URL.as("logoUrl"),
                        CLUBS.BANNER_URL.as("bannerUrl")
                )
                .from(CLUBS)
                .leftJoin(LOCATIONS).on(LOCATIONS.ENTITY_ID.eq(CLUBS.ID).and(LOCATIONS.ENTITY_TYPE.eq("CLUB")))
                .where(CLUBS.ID.eq(clubId))
                .fetchOptionalInto(ClubProfileDto.class);
    }

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
                .limit(1)
                .fetchOptionalInto(MyClubResponseDto.class);
    }

    public List<ClubProfileDto> getAllClubs(Long currentUserId) {
        return dsl.select(
                        CLUBS.ID, CLUBS.NAME, CLUBS.DESCRIPTION, CLUBS.TYPE, CLUBS.IS_OFFICIAL,
                        LOCATIONS.ADDRESS_TEXT,CLUBS.LOGO_URL, CLUBS.BANNER_URL,
                        DSL.field(DSL.selectCount().from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID))).as("followerCount"),
                        DSL.field(DSL.selectCount().from(CLUB_MEMBERSHIPS).where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))).as("memberCount"),
                        DSL.field(DSL.exists(DSL.selectOne().from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID).and(CLUB_FOLLOWS.USER_ID.eq(currentUserId))))).as("isFollowedByMe"),
                        DSL.field(DSL.exists(DSL.selectOne().from(CLUB_MEMBERSHIPS).where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID).and(CLUB_MEMBERSHIPS.USER_ID.eq(currentUserId))))).as("isMember")
                )
                .from(CLUBS)
                .leftJoin(LOCATIONS).on(LOCATIONS.ENTITY_ID.eq(CLUBS.ID).and(LOCATIONS.ENTITY_TYPE.eq("CLUB")))
                .orderBy(CLUBS.CREATED_AT.desc())
                .limit(50)
                .fetchInto(ClubProfileDto.class);
    }

    public List<ClubRosterDto> getClubRoster(Long clubId) {
        return dsl.select(
                        USERS.ID,
                        USER_PROFILES.FULL_NAME.as("name"),
                        USER_PROFILES.POSITION,
                        SQUAD_PLAYERS.JERSEY_NUMBER.as("number"),
                        USER_PROFILES.AVAILABILITY_STATUS.as("status")
                )
                .from(SQUAD_PLAYERS)
                .join(SQUADS).on(SQUADS.ID.eq(SQUAD_PLAYERS.SQUAD_ID))
                .join(USERS).on(USERS.ID.eq(SQUAD_PLAYERS.USER_ID))
                .leftJoin(USER_PROFILES).on(USER_PROFILES.USER_ID.eq(USERS.ID))
                .where(SQUADS.CLUB_ID.eq(clubId))
                .fetch(record -> {
                    String status = record.get("status", String.class);
                    String uiStatus = (status == null || status.equals("IN_CLUB")) ? "FIT" : "UNAVAILABLE";
                    return new ClubRosterDto(
                            record.get(USERS.ID),
                            record.get("name", String.class) != null ? record.get("name", String.class) : "Unknown Player",
                            record.get(USER_PROFILES.POSITION) != null ? record.get(USER_PROFILES.POSITION) : "RES",
                            record.get("number", Integer.class) != null ? record.get("number", Integer.class) : 99,
                            uiStatus,
                            "10b981"
                    );
                });
    }

    public List<ClubStaffDto> getClubStaff(Long clubId) {
        return dsl.select(
                        USERS.ID,
                        USER_PROFILES.FULL_NAME.as("name"),
                        CLUB_MEMBERSHIPS.ROLE
                )
                .from(CLUB_MEMBERSHIPS)
                .join(USERS).on(USERS.ID.eq(CLUB_MEMBERSHIPS.USER_ID))
                .leftJoin(USER_PROFILES).on(USER_PROFILES.USER_ID.eq(USERS.ID))
                .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(clubId))
                .and(CLUB_MEMBERSHIPS.ROLE.notEqual("PLAYER"))
                .fetch(record -> {
                    String role = record.get(CLUB_MEMBERSHIPS.ROLE);
                    String clearance = "LEVEL 2 (OPERATIONS)";
                    if ("CLUB_ADMIN".equals(role) || "OWNER".equals(role)) clearance = "LEVEL 5 (DIRECTOR)";
                    return new ClubStaffDto(
                            record.get(USERS.ID),
                            record.get("name", String.class) != null ? record.get("name", String.class) : "Staff Member",
                            role != null ? role.replace("_", " ") : "STAFF",
                            clearance
                    );
                });
    }


    public List<CalendarEventDto> getClubSchedule(Long clubId) {
        List<CalendarEventDto> events = new ArrayList<>();

        // Fetch Tryouts
        dsl.select(TRYOUTS.ID, TRYOUTS.TITLE, TRYOUTS.TRYOUT_DATE, LOCATIONS.ADDRESS_TEXT)
                .from(TRYOUTS)
                .leftJoin(LOCATIONS).on(TRYOUTS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(TRYOUTS.CLUB_ID.eq(clubId))
                .fetch().forEach(r -> events.add(new CalendarEventDto(
                        "TRYOUT-" + r.get(TRYOUTS.ID),
                        "TRYOUT",
                        r.get(TRYOUTS.TITLE),
                        r.get(TRYOUTS.TRYOUT_DATE).toLocalDate().toString(),
                        r.get(LOCATIONS.ADDRESS_TEXT) != null ? r.get(LOCATIONS.ADDRESS_TEXT) : "Base Camp",
                        "UPCOMING"
                )));

        // Fetch Match Requests
        dsl.select(MATCH_REQUESTS.ID, SQUADS.NAME, MATCH_REQUESTS.DESIRED_DATE, LOCATIONS.ADDRESS_TEXT, MATCH_REQUESTS.STATUS)
                .from(MATCH_REQUESTS)
                .join(SQUADS).on(MATCH_REQUESTS.SQUAD_ID.eq(SQUADS.ID))
                .leftJoin(LOCATIONS).on(MATCH_REQUESTS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(MATCH_REQUESTS.CLUB_ID.eq(clubId))
                .fetch().forEach(r -> events.add(new CalendarEventDto(
                        "MATCH-" + r.get(MATCH_REQUESTS.ID),
                        "MATCH",
                        "Match: " + r.get(SQUADS.NAME),
                        r.get(MATCH_REQUESTS.DESIRED_DATE).toLocalDate().toString(),
                        r.get(LOCATIONS.ADDRESS_TEXT) != null ? r.get(LOCATIONS.ADDRESS_TEXT) : "Base Camp",
                        r.get(MATCH_REQUESTS.STATUS)
                )));

        // NEW: Fetch Internal Club Events
        dsl.select(CLUB_EVENTS.ID, CLUB_EVENTS.TITLE, CLUB_EVENTS.EVENT_TYPE, CLUB_EVENTS.EVENT_DATE, CLUB_EVENTS.LOCATION_TEXT)
                .from(CLUB_EVENTS)
                .where(CLUB_EVENTS.CLUB_ID.eq(clubId))
                .fetch().forEach(r -> events.add(new CalendarEventDto(
                        "EVENT-" + r.get(CLUB_EVENTS.ID),
                        r.get(CLUB_EVENTS.EVENT_TYPE),
                        r.get(CLUB_EVENTS.TITLE),
                        r.get(CLUB_EVENTS.EVENT_DATE).toLocalDate().toString(),
                        r.get(CLUB_EVENTS.LOCATION_TEXT) != null ? r.get(CLUB_EVENTS.LOCATION_TEXT) : "Base Camp",
                        "UPCOMING"
                )));

        return events;
    }

    public void createCalendarEvent(Long clubId, Long userId, CalendarRequestDto request) {
        LocalDateTime eventDate = java.time.LocalDate.parse(request.date()).atTime(23, 59);

        // 1. Get Home Base Location
        Long baseLocationId = dsl.select(LOCATIONS.ID)
                .from(LOCATIONS)
                .where(LOCATIONS.ENTITY_ID.eq(clubId))
                .and(LOCATIONS.ENTITY_TYPE.eq("CLUB"))
                .limit(1)
                .fetchOneInto(Long.class);

        // 2. Override with Map Picker Location if provided!
        Long finalLocationId = baseLocationId;
        if (request.targetLocationClubId() != null) {
            Long targetLocId = dsl.select(LOCATIONS.ID)
                    .from(LOCATIONS)
                    .where(LOCATIONS.ENTITY_ID.eq(request.targetLocationClubId()))
                    .and(LOCATIONS.ENTITY_TYPE.eq("CLUB"))
                    .limit(1)
                    .fetchOneInto(Long.class);
            if (targetLocId != null) {
                finalLocationId = targetLocId;
            }
        }

        if ("TRYOUT".equals(request.type())) {
            dsl.insertInto(TRYOUTS)
                    .set(TRYOUTS.CLUB_ID, clubId)
                    .set(TRYOUTS.TITLE, request.title())
                    .set(TRYOUTS.DESCRIPTION, "Scheduled via Command Center")
                    .set(TRYOUTS.POSITION, "Any")
                    .set(TRYOUTS.AGE_GROUP, "Open")
                    .set(TRYOUTS.LOCATION_ID, finalLocationId) // Using the dynamic location!
                    .set(TRYOUTS.TRYOUT_DATE, eventDate)
                    .set(TRYOUTS.CREATED_BY, userId)
                    .execute();
        } else if ("MATCH".equals(request.type())) {
            Long squadId = dsl.select(SQUADS.ID).from(SQUADS).where(SQUADS.CLUB_ID.eq(clubId)).limit(1).fetchOneInto(Long.class);
            if (squadId != null) {
                dsl.insertInto(MATCH_REQUESTS)
                        .set(MATCH_REQUESTS.CLUB_ID, clubId)
                        .set(MATCH_REQUESTS.SQUAD_ID, squadId)
                        .set(MATCH_REQUESTS.CREATOR_ID, userId)
                        .set(MATCH_REQUESTS.DESIRED_DATE, eventDate)
                        .set(MATCH_REQUESTS.LOCATION_PREF, "ANY")
                        .set(MATCH_REQUESTS.LOCATION_ID, finalLocationId) // Using the dynamic location!
                        .set(MATCH_REQUESTS.STATUS, "OPEN")
                        .execute();
            }
        } else {
            dsl.insertInto(CLUB_EVENTS)
                    .set(CLUB_EVENTS.CLUB_ID, clubId)
                    .set(CLUB_EVENTS.TITLE, request.title())
                    .set(CLUB_EVENTS.EVENT_TYPE, request.type())
                    .set(CLUB_EVENTS.EVENT_DATE, eventDate)
                    .set(CLUB_EVENTS.LOCATION_TEXT, request.location())
                    .set(CLUB_EVENTS.CREATED_BY, userId)
                    .execute();
        }
    }

    // NEW: Update an existing event
    public void updateCalendarEvent(Long clubId, String eventIdString, CalendarRequestDto request) {
        LocalDateTime eventDate = java.time.LocalDate.parse(request.date()).atTime(23, 59);
        String[] parts = eventIdString.split("-");

        if (parts.length != 2) throw new IllegalArgumentException("Invalid Event ID format");

        String prefix = parts[0];
        Long id = Long.parseLong(parts[1]);

        if ("TRYOUT".equals(prefix)) {
            dsl.update(TRYOUTS)
                    .set(TRYOUTS.TITLE, request.title())
                    .set(TRYOUTS.TRYOUT_DATE, eventDate)
                    .where(TRYOUTS.ID.eq(id).and(TRYOUTS.CLUB_ID.eq(clubId)))
                    .execute();
        } else if ("MATCH".equals(prefix)) {
            dsl.update(MATCH_REQUESTS)
                    .set(MATCH_REQUESTS.DESIRED_DATE, eventDate)
                    .where(MATCH_REQUESTS.ID.eq(id).and(MATCH_REQUESTS.CLUB_ID.eq(clubId)))
                    .execute();
        } else if ("EVENT".equals(prefix)) {
            dsl.update(CLUB_EVENTS)
                    .set(CLUB_EVENTS.TITLE, request.title())
                    .set(CLUB_EVENTS.EVENT_TYPE, request.type())
                    .set(CLUB_EVENTS.EVENT_DATE, eventDate)
                    .set(CLUB_EVENTS.LOCATION_TEXT, request.location())
                    .where(CLUB_EVENTS.ID.eq(id).and(CLUB_EVENTS.CLUB_ID.eq(clubId)))
                    .execute();
        }
    }

    // NEW: Delete an existing event
    public void deleteCalendarEvent(Long clubId, String eventIdString) {
        String[] parts = eventIdString.split("-");

        if (parts.length != 2) throw new IllegalArgumentException("Invalid Event ID format");

        String prefix = parts[0];
        Long id = Long.parseLong(parts[1]);

        if ("TRYOUT".equals(prefix)) {
            dsl.deleteFrom(TRYOUTS).where(TRYOUTS.ID.eq(id).and(TRYOUTS.CLUB_ID.eq(clubId))).execute();
        } else if ("MATCH".equals(prefix)) {
            dsl.deleteFrom(MATCH_REQUESTS).where(MATCH_REQUESTS.ID.eq(id).and(MATCH_REQUESTS.CLUB_ID.eq(clubId))).execute();
        } else if ("EVENT".equals(prefix)) {
            dsl.deleteFrom(CLUB_EVENTS).where(CLUB_EVENTS.ID.eq(id).and(CLUB_EVENTS.CLUB_ID.eq(clubId))).execute();
        }
    }
}