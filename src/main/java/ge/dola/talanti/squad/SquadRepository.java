package ge.dola.talanti.squad;

import ge.dola.talanti.squad.dto.AddSquadPlayerDto;
import ge.dola.talanti.squad.dto.CreateSquadDto;
import ge.dola.talanti.squad.dto.SquadRosterPlayerDto;
import ge.dola.talanti.squad.dto.SquadDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.CLUBS;
import static ge.dola.talanti.jooq.Tables.PLAYER_DETAILS;
import static ge.dola.talanti.jooq.Tables.SQUADS;
import static ge.dola.talanti.jooq.Tables.SQUAD_PLAYERS;
import static ge.dola.talanti.jooq.Tables.USER_PROFILES;
import static ge.dola.talanti.jooq.Tables.USERS;

@Repository
@RequiredArgsConstructor
public class SquadRepository {

    private final DSLContext dsl;

    public Long createSquad(Long clubId, CreateSquadDto dto) {
        return dsl.insertInto(SQUADS)
                .set(SQUADS.CLUB_ID, clubId)
                .set(SQUADS.NAME, dto.name())
                .set(SQUADS.CATEGORY, dto.category())
                .set(SQUADS.GENDER, dto.gender())
                .set(SQUADS.CREATED_AT, LocalDateTime.now())
                .returningResult(SQUADS.ID)
                .fetchOneInto(Long.class);
    }

    public List<SquadDto> getSquadsForClub(Long clubId) {
        return dsl.select(
                        SQUADS.ID,
                        SQUADS.CLUB_ID,
                        SQUADS.NAME,
                        SQUADS.CATEGORY,
                        SQUADS.GENDER,
                        SQUADS.HEAD_COACH_ID
                )
                .from(SQUADS)
                .where(SQUADS.CLUB_ID.eq(clubId))
                .orderBy(SQUADS.CATEGORY.asc(), SQUADS.NAME.asc(), SQUADS.ID.asc())
                .fetchInto(SquadDto.class);
    }

    public List<SquadRosterPlayerDto> getSquadRoster(Long clubId, Long squadId) {
        return dsl.select(
                        USERS.ID,
                        SQUAD_PLAYERS.JERSEY_NUMBER.as("number"),
                        USER_PROFILES.FULL_NAME.as("name"),
                        PLAYER_DETAILS.PRIMARY_POSITION.as("position"),
                        DSL.field("date_part('year', age(current_date, {0}))", Integer.class, PLAYER_DETAILS.DATE_OF_BIRTH).as("age")
                )
                .from(SQUAD_PLAYERS)
                .join(SQUADS).on(SQUAD_PLAYERS.SQUAD_ID.eq(SQUADS.ID))
                .join(USERS).on(SQUAD_PLAYERS.USER_ID.eq(USERS.ID))
                .leftJoin(USER_PROFILES).on(USER_PROFILES.USER_ID.eq(USERS.ID))
                .leftJoin(PLAYER_DETAILS).on(PLAYER_DETAILS.USER_ID.eq(USERS.ID))
                .where(SQUADS.CLUB_ID.eq(clubId))
                .and(SQUADS.ID.eq(squadId))
                .orderBy(
                        SQUAD_PLAYERS.JERSEY_NUMBER.asc().nullsLast(),
                        USER_PROFILES.FULL_NAME.asc().nullsLast(),
                        USERS.ID.asc()
                )
                .fetchInto(SquadRosterPlayerDto.class);
    }

    public void addPlayerToSquad(Long squadId, AddSquadPlayerDto dto) {
        dsl.insertInto(SQUAD_PLAYERS)
                .set(SQUAD_PLAYERS.SQUAD_ID, squadId)
                .set(SQUAD_PLAYERS.USER_ID, dto.userId())
                .set(SQUAD_PLAYERS.JERSEY_NUMBER, dto.jerseyNumber())
                .set(SQUAD_PLAYERS.SQUAD_ROLE, dto.squadRole() != null ? dto.squadRole() : "PLAYER")
                .set(SQUAD_PLAYERS.JOINED_AT, LocalDateTime.now())
                .onDuplicateKeyUpdate()
                .set(SQUAD_PLAYERS.JERSEY_NUMBER, dto.jerseyNumber())
                .set(SQUAD_PLAYERS.SQUAD_ROLE, dto.squadRole() != null ? dto.squadRole() : "PLAYER")
                .execute();
    }

    public void removePlayerFromSquad(Long squadId, Long userId) {
        dsl.deleteFrom(SQUAD_PLAYERS)
                .where(SQUAD_PLAYERS.SQUAD_ID.eq(squadId))
                .and(SQUAD_PLAYERS.USER_ID.eq(userId))
                .execute();
    }

    public boolean doesSquadBelongToClub(Long clubId, Long squadId) {
        return dsl.fetchExists(
                dsl.selectOne().from(SQUADS)
                        .where(SQUADS.ID.eq(squadId))
                        .and(SQUADS.CLUB_ID.eq(clubId))
        );
    }

    public boolean existsByNormalizedName(Long clubId, String squadName) {
        if (squadName == null || squadName.isBlank()) {
            return false;
        }

        String normalizedName = squadName.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);

        return dsl.fetchExists(
                dsl.selectOne()
                        .from(SQUADS)
                        .where(SQUADS.CLUB_ID.eq(clubId))
                        .and(DSL.field(
                                "lower(regexp_replace(btrim({0}), '\\s+', ' ', 'g'))",
                                String.class,
                                SQUADS.NAME
                        ).eq(normalizedName))
        );
    }

    public Optional<SquadSummary> findSquadSummary(Long clubId, Long squadId) {
        return dsl.select(
                        SQUADS.ID,
                        SQUADS.CLUB_ID,
                        SQUADS.NAME,
                        CLUBS.NAME.as("clubName")
                )
                .from(SQUADS)
                .join(CLUBS).on(CLUBS.ID.eq(SQUADS.CLUB_ID))
                .where(SQUADS.ID.eq(squadId))
                .and(SQUADS.CLUB_ID.eq(clubId))
                .fetchOptional(record -> new SquadSummary(
                        record.get(SQUADS.ID),
                        record.get(SQUADS.CLUB_ID),
                        record.get(SQUADS.NAME),
                        record.get("clubName", String.class)
                ));
    }

    public record SquadSummary(Long squadId, Long clubId, String squadName, String clubName) {
    }
}
