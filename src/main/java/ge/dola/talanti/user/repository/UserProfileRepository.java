package ge.dola.talanti.user.repository;

import ge.dola.talanti.user.dto.CareerHistoryDto;
import ge.dola.talanti.user.dto.PublicUserProfileDto;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.*;

@Repository
public class UserProfileRepository {

    private final DSLContext dsl;

    public UserProfileRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<PublicUserProfileDto> getPublicProfile(Long targetUserId, Long currentUserId) {

        // 1. Fetch the player's career history first
        List<CareerHistoryDto> careerHistory = dsl.select(
                        CAREER_HISTORY.ID,
                        CAREER_HISTORY.CLUB_NAME,
                        CAREER_HISTORY.SEASON,
                        CAREER_HISTORY.CATEGORY,
                        CAREER_HISTORY.APPEARANCES,
                        CAREER_HISTORY.GOALS,
                        CAREER_HISTORY.ASSISTS,
                        CAREER_HISTORY.CLEAN_SHEETS
                )
                .from(CAREER_HISTORY)
                .where(CAREER_HISTORY.USER_ID.eq(targetUserId))
                .orderBy(CAREER_HISTORY.SEASON.desc())
                .fetchInto(CareerHistoryDto.class);

        // 2. Fetch the main profile and map it all together
        return dsl.select(
                        USERS.ID,
                        USERS.USERNAME,
                        USER_PROFILES.FULL_NAME,
                        USER_PROFILES.POSITION,
                        USER_PROFILES.PREFERRED_FOOT,
                        USER_PROFILES.BIO,

                        // NEW FIELDS
                        USER_PROFILES.AVAILABILITY_STATUS,
                        USER_PROFILES.HEIGHT_CM,
                        USER_PROFILES.WEIGHT_KG,

                        DSL.field(DSL.selectCount().from(FOLLOWS).where(FOLLOWS.FOLLOWING_ID.eq(USERS.ID))).as("followerCount"),
                        DSL.field(DSL.selectCount().from(FOLLOWS).where(FOLLOWS.FOLLOWER_ID.eq(USERS.ID))).as("followingCount"),
                        DSL.field(DSL.exists(DSL.selectOne().from(FOLLOWS).where(FOLLOWS.FOLLOWING_ID.eq(USERS.ID).and(FOLLOWS.FOLLOWER_ID.eq(currentUserId))))).as("isFollowedByMe")
                )
                .from(USERS)
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .where(USERS.ID.eq(targetUserId))
                .fetchOptional(record -> new PublicUserProfileDto(
                        record.get(USERS.ID),
                        record.get(USERS.USERNAME),
                        record.get(USER_PROFILES.FULL_NAME),
                        record.get(USER_PROFILES.POSITION),
                        record.get(USER_PROFILES.PREFERRED_FOOT),
                        record.get(USER_PROFILES.BIO),
                        record.get(USER_PROFILES.AVAILABILITY_STATUS),
                        record.get(USER_PROFILES.HEIGHT_CM),
                        record.get(USER_PROFILES.WEIGHT_KG),
                        record.get("followerCount", Integer.class),
                        record.get("followingCount", Integer.class),
                        record.get("isFollowedByMe", Boolean.class),
                        careerHistory // Inject the history list
                ));
    }
}