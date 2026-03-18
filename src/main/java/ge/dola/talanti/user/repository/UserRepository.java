package ge.dola.talanti.user.repository;

import ge.dola.talanti.jooq.tables.records.UsersRecord;
import ge.dola.talanti.user.dto.UserSearchDto;
import ge.dola.talanti.util.PageResult;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.*;

@Repository
public class UserRepository {

    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public boolean isFollowingUser(Long followerId, Long followingId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(FOLLOWS)
                        .where(FOLLOWS.FOLLOWER_ID.eq(followerId))
                        .and(FOLLOWS.FOLLOWING_ID.eq(followingId))
        );
    }

    public void followUser(Long followerId, Long followingId) {
        // We still provide the followerId for the explicit SQL INSERT statement,
        // but Postgres will reject it if it doesn't match the current_user_id context.
        dsl.insertInto(FOLLOWS)
                .set(FOLLOWS.FOLLOWER_ID, followerId)
                .set(FOLLOWS.FOLLOWING_ID, followingId)
                .set(FOLLOWS.CREATED_AT, LocalDateTime.now())
                .onDuplicateKeyIgnore()
                .execute();
    }

    public void unfollowUser(Long currentUserId, Long targetUserId) {
        dsl.deleteFrom(FOLLOWS)
                .where(FOLLOWS.FOLLOWER_ID.eq(currentUserId))
                .and(FOLLOWS.FOLLOWING_ID.eq(targetUserId)) // Always specify both sides of the relationship
                .execute();
    }

    public PageResult<UserSearchDto> searchUsers(String query, int page, int size) {
        String searchPattern = "%" + query + "%";

        Condition searchCondition = USER_PROFILES.FULL_NAME.likeIgnoreCase(searchPattern)
                .or(USERS.USERNAME.likeIgnoreCase(searchPattern));

        long total = dsl.selectCount()
                .from(USERS)
                .join(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .where(searchCondition)
                .fetchOne(0, Long.class);

        List<UserSearchDto> content = dsl.select(
                        USERS.ID,
                        USER_PROFILES.FULL_NAME,
                        USERS.USERNAME,
                        PLAYER_DETAILS.PRIMARY_POSITION
                )
                .from(USERS)
                .join(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .leftJoin(PLAYER_DETAILS).on(USERS.ID.eq(PLAYER_DETAILS.USER_ID))
                .where(searchCondition)
                .limit(size)
                .offset(page * size)
                .fetchInto(UserSearchDto.class);

        return new PageResult<>(content, page, size, total);
    }
    public Optional<UsersRecord> findById(Long id) {
        return dsl.selectFrom(USERS)
                .where(USERS.ID.eq(id))
                .fetchOptional();
    }

    public Optional<ge.dola.talanti.jooq.tables.records.UsersRecord> findByEmail(String email) {
        return dsl.selectFrom(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOptional();
    }

    public boolean existsByEmail(String email) {
        return dsl.fetchExists(
                dsl.selectOne().from(USERS).where(USERS.EMAIL.eq(email))
        );
    }

    public boolean existsByUsername(String username) {
        return dsl.fetchExists(
                dsl.selectOne().from(USERS).where(USERS.USERNAME.eq(username))
        );
    }
}