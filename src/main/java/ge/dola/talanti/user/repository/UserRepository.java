package ge.dola.talanti.user.repository;

import ge.dola.talanti.jooq.tables.records.UsersRecord;
import ge.dola.talanti.user.dto.UserSearchDto;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.USERS;
import static ge.dola.talanti.jooq.Tables.USER_PROFILES;
import static ge.dola.talanti.jooq.tables.Follows.FOLLOWS;

@Repository
public class UserRepository {

    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<UsersRecord> findByEmail(String email) {
        return dsl.selectFrom(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOptional();
    }

    public Optional<UsersRecord> findById(Long id) {
        return dsl.selectFrom(USERS)
                .where(USERS.ID.eq(id))
                .fetchOptional();
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
        dsl.insertInto(FOLLOWS)
                .set(FOLLOWS.FOLLOWER_ID, followerId)
                .set(FOLLOWS.FOLLOWING_ID, followingId)
                .set(FOLLOWS.CREATED_AT, LocalDateTime.now())
                .onDuplicateKeyIgnore()
                .execute();
    }

    public void unfollowUser(Long followerId, Long followingId) {
        dsl.deleteFrom(FOLLOWS)
                .where(FOLLOWS.FOLLOWER_ID.eq(followerId))
                .and(FOLLOWS.FOLLOWING_ID.eq(followingId))
                .execute();
    }

    public List<UserSearchDto> searchUsers(String query) {
        String searchPattern = "%" + query + "%";

        return dsl.select(
                        USERS.ID,
                        USER_PROFILES.FULL_NAME,
                        USERS.USERNAME,
                        USER_PROFILES.POSITION
                )
                .from(USERS)
                .join(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .where(USER_PROFILES.FULL_NAME.likeIgnoreCase(searchPattern)
                        .or(USERS.USERNAME.likeIgnoreCase(searchPattern)))
                .limit(10) // Only return top 10 results for the dropdown
                .fetch(record -> new UserSearchDto(
                        record.get(USERS.ID),
                        record.get(USER_PROFILES.FULL_NAME),
                        record.get(USERS.USERNAME),
                        record.get(USER_PROFILES.POSITION)
                ));
    }
}