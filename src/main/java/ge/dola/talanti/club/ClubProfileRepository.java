package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.ClubProfileDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.*;

@Repository
@RequiredArgsConstructor
public class ClubProfileRepository {

    private final DSLContext dsl;

    public Optional<ClubProfileDto> getClubProfile(Long clubId, Long currentUserId) {
        return dsl.select(
                        CLUBS.ID,
                        CLUBS.NAME,
                        CLUBS.DESCRIPTION,
                        CLUBS.TYPE,
                        CLUBS.IS_OFFICIAL,

                        // Subquery: Total Followers
                        DSL.field(
                                DSL.selectCount().from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID))
                        ).as("followerCount"),

                        // Subquery: Total Members
                        DSL.field(
                                DSL.selectCount().from(CLUB_MEMBERSHIPS).where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))
                        ).as("memberCount"),

                        // FIX: Use DSL.exists() instead of casting count()
                        DSL.field(
                                DSL.exists(
                                        DSL.selectOne().from(CLUB_FOLLOWS)
                                                .where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID)
                                                        .and(CLUB_FOLLOWS.USER_ID.eq(currentUserId)))
                                )
                        ).as("isFollowedByMe"),

                        // FIX: Use DSL.exists() instead of casting count()
                        DSL.field(
                                DSL.exists(
                                        DSL.selectOne().from(CLUB_MEMBERSHIPS)
                                                .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID)
                                                        .and(CLUB_MEMBERSHIPS.USER_ID.eq(currentUserId)))
                                )
                        ).as("isMember")
                )
                .from(CLUBS)
                .where(CLUBS.ID.eq(clubId))
                .fetchOptional(record -> new ClubProfileDto(
                        record.get(CLUBS.ID),
                        record.get(CLUBS.NAME),
                        record.get(CLUBS.DESCRIPTION),
                        record.get(CLUBS.TYPE),
                        record.get(CLUBS.IS_OFFICIAL),
                        record.get("followerCount", Integer.class),
                        record.get("memberCount", Integer.class),
                        record.get("isFollowedByMe", Boolean.class),
                        record.get("isMember", Boolean.class)
                ));
    }
}