package ge.dola.talanti.user;

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
                        USERS.ID,
                        USERS.USERNAME,
                        USER_PROFILES.FULL_NAME,
                        USER_PROFILES.POSITION,
                        USER_PROFILES.PREFERRED_FOOT,
                        USER_PROFILES.BIO,

                        // Subquery: Total people following this user
                        DSL.field(
                                DSL.selectCount().from(FOLLOWS).where(FOLLOWS.FOLLOWING_ID.eq(USERS.ID))
                        ).as("followerCount"),

                        // Subquery: Total people this user is following
                        DSL.field(
                                DSL.selectCount().from(FOLLOWS).where(FOLLOWS.FOLLOWER_ID.eq(USERS.ID))
                        ).as("followingCount"),

                        // Subquery: Does the current logged-in user follow them?
                        DSL.field(
                                DSL.exists(
                                        DSL.selectOne().from(FOLLOWS)
                                                .where(FOLLOWS.FOLLOWING_ID.eq(USERS.ID)
                                                        .and(FOLLOWS.FOLLOWER_ID.eq(currentUserId)))
                                )
                        ).as("isFollowedByMe")
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
                        record.get("followerCount", Integer.class),
                        record.get("followingCount", Integer.class),
                        record.get("isFollowedByMe", Boolean.class)
                ));
    }
}