package ge.dola.talanti.club;

import ge.dola.talanti.jooq.tables.records.ClubsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.*;
import static ge.dola.talanti.jooq.tables.ClubFollows.CLUB_FOLLOWS;
import static ge.dola.talanti.jooq.tables.UserProfiles.USER_PROFILES;

@Repository
public class ClubRepository {

    private final DSLContext dsl;

    public ClubRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<ClubsRecord> findById(Long id) {
        return dsl.selectFrom(CLUBS)
                .where(CLUBS.ID.eq(id))
                .fetchOptional();
    }

    public List<ClubsRecord> findAll() {
        return dsl.selectFrom(CLUBS).fetch();
    }

    public ClubsRecord save(ClubsRecord club) {
        // JOOQ records can execute themselves if attached,
        // but explicit DSL execution is often cleaner.
        return dsl.insertInto(CLUBS)
                .set(club)
                .returning()
                .fetchOne();
    }

    public boolean isUserFollowingClub(Long userId, Long clubId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(CLUB_FOLLOWS)
                        .where(CLUB_FOLLOWS.USER_ID.eq(userId))
                        .and(CLUB_FOLLOWS.CLUB_ID.eq(clubId))
        );
    }



    public void followClub(Long userId, Long clubId) {
        dsl.insertInto(CLUB_FOLLOWS)
                .set(CLUB_FOLLOWS.USER_ID, userId)
                .set(CLUB_FOLLOWS.CLUB_ID, clubId)
                .set(CLUB_FOLLOWS.CREATED_AT, LocalDateTime.now())
                .onDuplicateKeyIgnore() // Safety net: prevents crashing if they double-click fast
                .execute();
    }

    public void unfollowClub(Long userId, Long clubId) {
        dsl.deleteFrom(CLUB_FOLLOWS)
                .where(CLUB_FOLLOWS.USER_ID.eq(userId))
                .and(CLUB_FOLLOWS.CLUB_ID.eq(clubId))
                .execute();
    }

}