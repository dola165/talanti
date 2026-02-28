package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.ClubProfileDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
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
                        LOCATIONS.ADDRESS_TEXT,

                        // Subquery: Total Followers
                        DSL.field(
                                DSL.selectCount().from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID))
                        ).as("followerCount"),

                        // Subquery: Total Members
                        DSL.field(
                                DSL.selectCount().from(CLUB_MEMBERSHIPS).where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))
                        ).as("memberCount"),

                        // Use DSL.exists() instead of casting count()
                        DSL.field(
                                DSL.exists(
                                        DSL.selectOne().from(CLUB_FOLLOWS)
                                                .where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID)
                                                        .and(CLUB_FOLLOWS.USER_ID.eq(currentUserId)))
                                )
                        ).as("isFollowedByMe"),

                        // Use DSL.exists() instead of casting count()
                        DSL.field(
                                DSL.exists(
                                        DSL.selectOne().from(CLUB_MEMBERSHIPS)
                                                .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID)
                                                        .and(CLUB_MEMBERSHIPS.USER_ID.eq(currentUserId)))
                                )
                        ).as("isMember")
                )
                .from(CLUBS)
                // THE MISSING LINE THAT CAUSED THE CRASH:
                .leftJoin(LOCATIONS).on(LOCATIONS.ENTITY_ID.eq(CLUBS.ID).and(LOCATIONS.ENTITY_TYPE.eq("CLUB")))
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
                        record.get("isMember", Boolean.class),
                        record.get(LOCATIONS.ADDRESS_TEXT)
                ));
    }

    // NEW METHOD: Fetch all clubs for the directory
    public List<ClubProfileDto> getAllClubs(Long currentUserId) {
        return dsl.select(
                        CLUBS.ID,
                        CLUBS.NAME,
                        CLUBS.DESCRIPTION,
                        CLUBS.TYPE,
                        CLUBS.IS_OFFICIAL,
                        LOCATIONS.ADDRESS_TEXT,
                        DSL.field(DSL.selectCount().from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID))).as("followerCount"),
                        DSL.field(DSL.selectCount().from(CLUB_MEMBERSHIPS).where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID))).as("memberCount"),
                        DSL.field(DSL.exists(DSL.selectOne().from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.CLUB_ID.eq(CLUBS.ID).and(CLUB_FOLLOWS.USER_ID.eq(currentUserId))))).as("isFollowedByMe"),
                        DSL.field(DSL.exists(DSL.selectOne().from(CLUB_MEMBERSHIPS).where(CLUB_MEMBERSHIPS.CLUB_ID.eq(CLUBS.ID).and(CLUB_MEMBERSHIPS.USER_ID.eq(currentUserId))))).as("isMember")
                )
                .from(CLUBS)
                .leftJoin(LOCATIONS).on(LOCATIONS.ENTITY_ID.eq(CLUBS.ID).and(LOCATIONS.ENTITY_TYPE.eq("CLUB")))
                .orderBy(CLUBS.CREATED_AT.desc())
                .limit(50) // Safe limit for MVP
                .fetch(record -> new ClubProfileDto(
                        record.get(CLUBS.ID), record.get(CLUBS.NAME), record.get(CLUBS.DESCRIPTION),
                        record.get(CLUBS.TYPE), record.get(CLUBS.IS_OFFICIAL),
                        record.get("followerCount", Integer.class), record.get("memberCount", Integer.class),
                        record.get("isFollowedByMe", Boolean.class), record.get("isMember", Boolean.class),
                        record.get(LOCATIONS.ADDRESS_TEXT)
                ));
    }
}