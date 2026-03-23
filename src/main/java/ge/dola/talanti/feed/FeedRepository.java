package ge.dola.talanti.feed;

import ge.dola.talanti.feed.dto.FeedPostDto;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.springframework.stereotype.Repository;
import org.jooq.impl.DSL;

import java.util.Arrays;
import java.util.List;

import static ge.dola.talanti.jooq.Tables.CLUBS;
import static ge.dola.talanti.jooq.Tables.CLUB_MEMBERSHIPS;
import static ge.dola.talanti.jooq.Tables.COMMENTS;
import static ge.dola.talanti.jooq.Tables.MEDIA;
import static ge.dola.talanti.jooq.Tables.POSTS;
import static ge.dola.talanti.jooq.Tables.POST_MEDIA;
import static ge.dola.talanti.jooq.Tables.USERS;
import static ge.dola.talanti.jooq.Tables.USER_PROFILES;
import static ge.dola.talanti.jooq.tables.Likes.LIKES;

@Repository
@RequiredArgsConstructor
public class FeedRepository {

    private final DSLContext dsl;

    public List<FeedPostDto> getFeedForUser(Long currentUserId, Long cursor, int limit) {
        var query = baseFeedQuery(currentUserId)
                .and(POSTS.IS_PUBLIC.eq(true));

        if (cursor != null) {
            query.and(POSTS.ID.lessThan(cursor));
        }

        return query.orderBy(POSTS.ID.desc())
                .limit(limit)
                .fetch(this::toFeedPostDto);
    }

    public List<FeedPostDto> getClubFeed(Long clubId, Long currentUserId, Long cursor, int limit) {
        var query = baseFeedQuery(currentUserId)
                .and(POSTS.CLUB_ID.eq(clubId))
                .and(clubFeedVisibility(clubId, currentUserId));

        if (cursor != null) {
            query.and(POSTS.ID.lessThan(cursor));
        }

        return query.orderBy(POSTS.ID.desc())
                .limit(limit)
                .fetch(this::toFeedPostDto);
    }

    public List<FeedPostDto> getUserFeed(Long authorId, Long currentUserId, Long cursor, int limit) {
        var query = baseFeedQuery(currentUserId)
                .and(POSTS.AUTHOR_ID.eq(authorId))
                .and(userFeedVisibility(authorId, currentUserId));

        if (cursor != null) {
            query.and(POSTS.ID.lessThan(cursor));
        }

        return query.orderBy(POSTS.ID.desc())
                .limit(limit)
                .fetch(this::toFeedPostDto);
    }

    public List<ge.dola.talanti.feed.dto.CommentDto> getCommentsForPost(Long postId) {
        return dsl.select(
                        COMMENTS.ID,
                        DSL.coalesce(USER_PROFILES.FULL_NAME, USERS.USERNAME).as("authorName"),
                        USER_PROFILES.PROFILE_PICTURE_URL.as("authorAvatarUrl"),
                        COMMENTS.CONTENT,
                        COMMENTS.CREATED_AT
                )
                .from(COMMENTS)
                .join(USERS).on(COMMENTS.USER_ID.eq(USERS.ID))
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .where(COMMENTS.POST_ID.eq(postId))
                .orderBy(COMMENTS.CREATED_AT.asc(), COMMENTS.ID.asc())
                .fetchInto(ge.dola.talanti.feed.dto.CommentDto.class);
    }

    private SelectConditionStep<?> baseFeedQuery(Long currentUserId) {
        return dsl.select(
                        POSTS.ID,
                        POSTS.CONTENT,
                        POSTS.CREATED_AT,
                        USERS.ID.as("authorId"),
                        DSL.coalesce(USER_PROFILES.FULL_NAME, USERS.USERNAME).as("authorName"),
                        DSL.coalesce(CLUBS.LOGO_URL, USER_PROFILES.PROFILE_PICTURE_URL).as("authorAvatarUrl"),
                        CLUBS.ID.as("clubId"),
                        CLUBS.NAME.as("clubName"),
                        DSL.field(DSL.selectCount().from(LIKES).where(LIKES.POST_ID.eq(POSTS.ID))).as("likeCount"),
                        DSL.field(DSL.selectCount().from(COMMENTS).where(COMMENTS.POST_ID.eq(POSTS.ID))).as("commentCount"),
                        DSL.field(DSL.exists(
                                DSL.selectOne()
                                        .from(LIKES)
                                        .where(LIKES.POST_ID.eq(POSTS.ID))
                                        .and(LIKES.USER_ID.eq(currentUserId))
                        )).as("isLikedByMe"),
                        DSL.field(
                                DSL.select(DSL.arrayAgg(MEDIA.URL))
                                        .from(POST_MEDIA)
                                        .join(MEDIA).on(POST_MEDIA.MEDIA_ID.eq(MEDIA.ID))
                                        .where(POST_MEDIA.POST_ID.eq(POSTS.ID))
                        ).as("mediaUrls")
                )
                .from(POSTS)
                .leftJoin(USERS).on(POSTS.AUTHOR_ID.eq(USERS.ID))
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .leftJoin(CLUBS).on(POSTS.CLUB_ID.eq(CLUBS.ID))
                .where(DSL.trueCondition());
    }

    private Condition clubFeedVisibility(Long clubId, Long currentUserId) {
        Condition visibility = POSTS.IS_PUBLIC.eq(true);
        if (hasAuthenticatedContext(currentUserId)) {
            visibility = visibility
                    .or(POSTS.AUTHOR_ID.eq(currentUserId))
                    .or(DSL.exists(
                            DSL.selectOne()
                                    .from(CLUB_MEMBERSHIPS)
                                    .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(clubId))
                                    .and(CLUB_MEMBERSHIPS.USER_ID.eq(currentUserId))
                                    .and(CLUB_MEMBERSHIPS.ROLE.in("OWNER", "CLUB_ADMIN"))
                    ));
        }
        return visibility;
    }

    private Condition userFeedVisibility(Long authorId, Long currentUserId) {
        Condition visibility = POSTS.IS_PUBLIC.eq(true);
        if (hasAuthenticatedContext(currentUserId) && currentUserId.equals(authorId)) {
            visibility = visibility.or(POSTS.AUTHOR_ID.eq(currentUserId));
        }
        return visibility;
    }

    private boolean hasAuthenticatedContext(Long currentUserId) {
        return currentUserId != null && currentUserId > 0;
    }

    private FeedPostDto toFeedPostDto(Record record) {
        String[] mediaArray = record.get("mediaUrls", String[].class);
        List<String> mediaList = mediaArray != null ? Arrays.asList(mediaArray) : List.of();

        return new FeedPostDto(
                record.get(POSTS.ID),
                record.get(POSTS.CONTENT),
                record.get(POSTS.CREATED_AT),
                record.get("authorId", Long.class),
                record.get("authorName", String.class),
                record.get("authorAvatarUrl", String.class),
                record.get("clubId", Long.class),
                record.get("clubName", String.class),
                record.get("likeCount", Integer.class),
                record.get("commentCount", Integer.class),
                record.get("isLikedByMe", Boolean.class),
                mediaList
        );
    }
}
