package ge.dola.talanti.user.repository;

import ge.dola.talanti.club.ClubDynamicTables;
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

    public record ProfileSummary(Long id, String username, String role, String fullName) {
    }

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
        if (query == null || query.trim().length() < 2) {
            return new PageResult<>(List.of(), page, size, 0);
        }
        String searchPattern = "%" + query + "%";

        Condition searchCondition = USER_PROFILES.FULL_NAME.likeIgnoreCase(searchPattern)
                .or(USERS.USERNAME.likeIgnoreCase(searchPattern));

        return searchUsersInternal(searchCondition, page, size);
    }

    public PageResult<UserSearchDto> searchUsersForClubInvite(Long clubId, Long actorUserId, String query, int page, int size) {
        if (query == null || query.trim().length() < 2) {
            return new PageResult<>(List.of(), page, size, 0);
        }
        String searchPattern = "%" + query + "%";

        Condition searchCondition = USER_PROFILES.FULL_NAME.likeIgnoreCase(searchPattern)
                .or(USERS.USERNAME.likeIgnoreCase(searchPattern));

        Condition inviteSearchCondition = searchCondition
                .and(USERS.ID.ne(actorUserId))
                .and(USERS.USER_TYPE.ne("SYSTEM_ADMIN"))
                .andNotExists(
                        dsl.selectOne()
                                .from(CLUB_MEMBERSHIPS)
                                .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(clubId))
                                .and(CLUB_MEMBERSHIPS.USER_ID.eq(USERS.ID))
                )
                .andNotExists(
                        dsl.selectOne()
                                .from(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES)
                                .where(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_CLUB_ID.eq(clubId))
                                .and(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_INVITEE_USER_ID.eq(USERS.ID))
                                .and(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_STATUS.eq("PENDING"))
                );

        return searchUsersInternal(inviteSearchCondition, page, size);
    }

    private PageResult<UserSearchDto> searchUsersInternal(Condition condition, int page, int size) {
        long total = dsl.selectCount()
                .from(USERS)
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .where(condition)
                .fetchOne(0, Long.class);

        List<UserSearchDto> content = dsl.select(
                        USERS.ID,
                        USER_PROFILES.FULL_NAME,
                        USERS.USERNAME,
                        PLAYER_DETAILS.PRIMARY_POSITION,
                        USERS.USER_TYPE,
                        USER_PROFILES.PROFILE_PICTURE_URL.as("avatarUrl")
                )
                .from(USERS)
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .leftJoin(PLAYER_DETAILS).on(USERS.ID.eq(PLAYER_DETAILS.USER_ID))
                .where(condition)
                .orderBy(USER_PROFILES.FULL_NAME.asc().nullsLast(), USERS.USERNAME.asc(), USERS.ID.asc())
                .limit(size)
                .offset(page * size)
                .fetchInto(UserSearchDto.class);

        return new PageResult<>(content, page, size, total);
    }

    public Optional<ProfileSummary> findProfileSummary(Long userId) {
        return dsl.select(
                        USERS.ID,
                        USERS.USERNAME,
                        USERS.USER_TYPE,
                        USER_PROFILES.FULL_NAME
                )
                .from(USERS)
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .where(USERS.ID.eq(userId))
                .fetchOptional(record -> new ProfileSummary(
                        record.get(USERS.ID),
                        record.get(USERS.USERNAME),
                        record.get(USERS.USER_TYPE),
                        record.get(USER_PROFILES.FULL_NAME)
                ));
    }

    public Optional<String> findDisplayNameById(Long userId) {
        return dsl.select(
                        USER_PROFILES.FULL_NAME,
                        USERS.USERNAME
                )
                .from(USERS)
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .where(USERS.ID.eq(userId))
                .fetchOptional(record -> {
                    String fullName = record.get(USER_PROFILES.FULL_NAME);
                    if (fullName != null && !fullName.isBlank()) {
                        return fullName;
                    }
                    return record.get(USERS.USERNAME);
                });
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
