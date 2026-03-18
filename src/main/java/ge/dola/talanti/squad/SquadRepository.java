package ge.dola.talanti.squad;

import ge.dola.talanti.squad.dto.AddSquadPlayerDto;
import ge.dola.talanti.squad.dto.CreateSquadDto;
import ge.dola.talanti.squad.dto.SquadDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static ge.dola.talanti.jooq.Tables.SQUADS;
import static ge.dola.talanti.jooq.Tables.SQUAD_PLAYERS;

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
        return dsl.selectFrom(SQUADS)
                .where(SQUADS.CLUB_ID.eq(clubId))
                .fetchInto(SquadDto.class);
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
}