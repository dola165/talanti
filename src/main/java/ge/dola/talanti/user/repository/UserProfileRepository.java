package ge.dola.talanti.user.repository;

import ge.dola.talanti.user.UserType;
import ge.dola.talanti.user.dto.PublicUserProfileDto;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.*;

@Repository
public class UserProfileRepository {

    private final DSLContext dsl;

    public UserProfileRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<PublicUserProfileDto> getPublicProfile(Long targetUserId, Long currentUserId) {
        return dsl.select(
                        USERS.ID, USERS.USERNAME, USERS.USER_TYPE,
                        USER_PROFILES.FULL_NAME, USER_PROFILES.BIO,
                        USER_PROFILES.PROFILE_PICTURE_URL, USER_PROFILES.BANNER_URL,
                        PLAYER_DETAILS.PRIMARY_POSITION, PLAYER_DETAILS.PREFERRED_FOOT,
                        PLAYER_DETAILS.AVAILABILITY_STATUS,
                        PLAYER_DETAILS.HEIGHT_CM, PLAYER_DETAILS.WEIGHT_KG,
                        DSL.field(DSL.selectCount().from(FOLLOWS).where(FOLLOWS.FOLLOWING_ID.eq(USERS.ID))).as("followerCount"),
                        DSL.field(DSL.selectCount().from(FOLLOWS).where(FOLLOWS.FOLLOWER_ID.eq(USERS.ID))).as("followingCount"),
                        DSL.field(DSL.exists(DSL.selectOne().from(FOLLOWS)
                                .where(FOLLOWS.FOLLOWING_ID.eq(USERS.ID))
                                .and(FOLLOWS.FOLLOWER_ID.eq(currentUserId)))).as("isFollowedByMe")
                )
                .from(USERS)
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .leftJoin(PLAYER_DETAILS).on(USERS.ID.eq(PLAYER_DETAILS.USER_ID))
                .where(USERS.ID.eq(targetUserId))
                .fetchOptional(record -> {
                    String roleStr = record.get(USERS.USER_TYPE);
                    UserType roleEnum = roleStr != null ? UserType.valueOf(roleStr) : UserType.FAN;

                    return new PublicUserProfileDto(
                            record.get(USERS.ID),
                            record.get(USERS.USERNAME),
                            record.get(USER_PROFILES.FULL_NAME),
                            roleEnum, // Mapped securely
                            record.get(PLAYER_DETAILS.PRIMARY_POSITION),
                            record.get(PLAYER_DETAILS.PREFERRED_FOOT),
                            record.get(USER_PROFILES.BIO),
                            record.get(PLAYER_DETAILS.AVAILABILITY_STATUS),
                            record.get(PLAYER_DETAILS.HEIGHT_CM),
                            record.get(PLAYER_DETAILS.WEIGHT_KG),
                            record.get("followerCount", Integer.class),
                            record.get("followingCount", Integer.class),
                            record.get("isFollowedByMe", Boolean.class),
                            record.get(USER_PROFILES.PROFILE_PICTURE_URL),
                            record.get(USER_PROFILES.BANNER_URL)
                    );
                });
    }
}
