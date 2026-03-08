package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MatchRequestDto;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static ge.dola.talanti.jooq.Tables.*;

@Service
public class MatchFinderService {

    private final DSLContext dsl;

    public MatchFinderService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void createMatchRequest(Long currentUserId, MatchRequestDto dto) {
        // --- 1. STRICT ACCESS CONTROL CHECK ---
        Long clubOwnerId = dsl.select(CLUBS.CREATED_BY)
                .from(CLUBS)
                .where(CLUBS.ID.eq(dto.clubId()))
                .fetchOneInto(Long.class);

        if (clubOwnerId == null || !clubOwnerId.equals(currentUserId)) {
            throw new RuntimeException("ACCESS DENIED: Only the club administrator can post match requests.");
        }

        // --- 2. GET CLUB LOCATION (If they are hosting) ---
        Long locationId = dsl.select(CLUBS.LOCATION_ID)
                .from(CLUBS)
                .where(CLUBS.ID.eq(dto.clubId()))
                .fetchOneInto(Long.class);

        // --- 3. SAVE THE REQUEST ---
        dsl.insertInto(MATCH_REQUESTS)
                .set(MATCH_REQUESTS.CLUB_ID, dto.clubId())
                .set(MATCH_REQUESTS.SQUAD_ID, dto.squadId())
                .set(MATCH_REQUESTS.CREATOR_ID, currentUserId)
                .set(MATCH_REQUESTS.DESIRED_DATE, dto.desiredDate())
                .set(MATCH_REQUESTS.LOCATION_PREF, dto.locationPref())
                .set(MATCH_REQUESTS.LOCATION_ID, locationId)
                .set(MATCH_REQUESTS.STATUS, "OPEN")
                .set(MATCH_REQUESTS.CREATED_AT, LocalDateTime.now())
                .execute();
    }
}