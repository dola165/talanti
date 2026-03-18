package ge.dola.talanti.feed;

import ge.dola.talanti.feed.dto.FeedPostDto;
import ge.dola.talanti.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import java.util.List;

import static ge.dola.talanti.jooq.Tables.*;

@Repository
@RequiredArgsConstructor
public class FeedRepository {

    private final DSLContext dsl;
    private final ApplicationEventPublisher applicationEventPublisher;


    public List<FeedPostDto> getFeedForUser(Long currentUserId, Long cursor, int limit) {

        var query = dsl.select(
                        POSTS.ID,
                        POSTS.CONTENT,
                        POSTS.CREATED_AT,
                        USERS.ID.as("authorId"),
                        DSL.coalesce(USER_PROFILES.FULL_NAME, USERS.USERNAME).as("authorName"),
                        CLUBS.ID.as("clubId"),
                        CLUBS.NAME.as("clubName"),

                        // Get total likes
                        DSL.field(DSL.selectCount().from(LIKES).where(LIKES.POST_ID.eq(POSTS.ID))).as("likeCount"),
                        // Get total comments
                        DSL.field(DSL.selectCount().from(COMMENTS).where(COMMENTS.POST_ID.eq(POSTS.ID))).as("commentCount"),
                        // Check if I liked it
                        DSL.field(DSL.exists(
                                DSL.selectOne().from(LIKES).where(LIKES.POST_ID.eq(POSTS.ID).and(LIKES.USER_ID.eq(currentUserId)))
                        )).as("isLikedByMe"),

                        // Fetch the media URLs as a PostgreSQL Array!
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
                .where(POSTS.IS_PUBLIC.eq(true));

        if (cursor != null) {
            query.and(POSTS.ID.lessThan(cursor));
        }

        return query.orderBy(POSTS.ID.desc())
                .limit(limit)
                .fetch(record -> {
                    // Safely extract the PostgreSQL Array and convert it to a Java List
                    String[] mediaArray = record.get("mediaUrls", String[].class);
                    List<String> mediaList = mediaArray != null ? java.util.Arrays.asList(mediaArray) : java.util.List.of();

                    return new FeedPostDto(
                            record.get(POSTS.ID),
                            record.get(POSTS.CONTENT),
                            record.get(POSTS.CREATED_AT),
                            record.get("authorId", Long.class),
                            record.get("authorName", String.class),
                            record.get("clubId", Long.class),
                            record.get("clubName", String.class),
                            record.get("likeCount", Integer.class),
                            record.get("commentCount", Integer.class),
                            record.get("isLikedByMe", Boolean.class),
                            mediaList // Pass it into the DTO!
                    );
                });
    }

    // NEW METHOD: Fetch posts for a specific club
    public List<FeedPostDto> getClubFeed(Long clubId, Long currentUserId, Long cursor, int limit) {
        var query = dsl.select(
                        POSTS.ID,
                        POSTS.CONTENT,
                        POSTS.CREATED_AT,
                        USERS.ID.as("authorId"),
                        DSL.coalesce(USER_PROFILES.FULL_NAME, USERS.USERNAME).as("authorName"),
                        CLUBS.ID.as("clubId"),
                        CLUBS.NAME.as("clubName"),
                        DSL.field(DSL.selectCount().from(LIKES).where(LIKES.POST_ID.eq(POSTS.ID))).as("likeCount"),
                        DSL.field(DSL.selectCount().from(COMMENTS).where(COMMENTS.POST_ID.eq(POSTS.ID))).as("commentCount"),
                        DSL.field(
                                DSL.select(DSL.arrayAgg(MEDIA.URL))
                                        .from(POST_MEDIA)
                                        .join(MEDIA).on(POST_MEDIA.MEDIA_ID.eq(MEDIA.ID))
                                        .where(POST_MEDIA.POST_ID.eq(POSTS.ID))
                        ).as("mediaUrls"),
                        DSL.field(DSL.exists(
                                DSL.selectOne().from(LIKES)
                                        .where(LIKES.POST_ID.eq(POSTS.ID)
                                                .and(LIKES.USER_ID.eq(currentUserId)))
                        )).as("isLikedByMe")
                )
                .from(POSTS)
                .leftJoin(USERS).on(POSTS.AUTHOR_ID.eq(USERS.ID))
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .leftJoin(CLUBS).on(POSTS.CLUB_ID.eq(CLUBS.ID))
                .where(POSTS.CLUB_ID.eq(clubId)); // <-- THE MAGIC FILTER

        if (cursor != null) {
            query.and(POSTS.ID.lessThan(cursor));
        }

        return query.orderBy(POSTS.ID.desc())
                .limit(limit)
                .fetch(record -> new FeedPostDto(
                        record.get(POSTS.ID), record.get(POSTS.CONTENT), record.get(POSTS.CREATED_AT),
                        record.get("authorId", Long.class), record.get("authorName", String.class),
                        record.get("clubId", Long.class), record.get("clubName", String.class),
                        record.get("likeCount", Integer.class), record.get("commentCount", Integer.class),
                        record.get("isLikedByMe", Boolean.class),
                        record.get("mediaUrls", String[].class) != null
                        ? java.util.Arrays.asList(record.get("mediaUrls", String[].class))
                        : java.util.List.of()
                ));
    }

    // NEW METHOD: Fetch posts for a specific user
    public List<FeedPostDto> getUserFeed(Long authorId, Long currentUserId, Long cursor, int limit) {
        var query = dsl.select(
                        POSTS.ID,
                        POSTS.CONTENT,
                        POSTS.CREATED_AT,
                        USERS.ID.as("authorId"),
                        DSL.coalesce(USER_PROFILES.FULL_NAME, USERS.USERNAME).as("authorName"),
                        CLUBS.ID.as("clubId"),
                        CLUBS.NAME.as("clubName"),
                        DSL.field(DSL.selectCount().from(LIKES).where(LIKES.POST_ID.eq(POSTS.ID))).as("likeCount"),
                        DSL.field(DSL.selectCount().from(COMMENTS).where(COMMENTS.POST_ID.eq(POSTS.ID))).as("commentCount"),
                        DSL.field(
                                DSL.select(DSL.arrayAgg(MEDIA.URL))
                                        .from(POST_MEDIA)
                                        .join(MEDIA).on(POST_MEDIA.MEDIA_ID.eq(MEDIA.ID))
                                        .where(POST_MEDIA.POST_ID.eq(POSTS.ID))
                        ).as("mediaUrls"),
                        DSL.field(DSL.exists(
                                DSL.selectOne().from(LIKES)
                                        .where(LIKES.POST_ID.eq(POSTS.ID)
                                                .and(LIKES.USER_ID.eq(currentUserId)))
                        )).as("isLikedByMe")
                )
                .from(POSTS)
                .leftJoin(USERS).on(POSTS.AUTHOR_ID.eq(USERS.ID))
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .leftJoin(CLUBS).on(POSTS.CLUB_ID.eq(CLUBS.ID))
                .where(POSTS.AUTHOR_ID.eq(authorId));

        if (cursor != null) {
            query.and(POSTS.ID.lessThan(cursor));
        }

        return query.orderBy(POSTS.ID.desc())
                .limit(limit)
                .fetch(record -> new FeedPostDto(
                        record.get(POSTS.ID), record.get(POSTS.CONTENT), record.get(POSTS.CREATED_AT),
                        record.get("authorId", Long.class), record.get("authorName", String.class),
                        record.get("clubId", Long.class), record.get("clubName", String.class),
                        record.get("likeCount", Integer.class), record.get("commentCount", Integer.class),
                        record.get("isLikedByMe", Boolean.class),
                        record.get("mediaUrls", String[].class) != null
                                ? java.util.Arrays.asList(record.get("mediaUrls", String[].class))
                                : java.util.List.of()
                ));
    }



    // --- COMMENTS ---
    public java.util.List<ge.dola.talanti.feed.dto.CommentDto> getCommentsForPost(Long postId) {
        return dsl.select(
                        COMMENTS.ID,
                        DSL.coalesce(USER_PROFILES.FULL_NAME, USERS.USERNAME).as("authorName"),
                        COMMENTS.CONTENT,
                        COMMENTS.CREATED_AT
                )
                .from(COMMENTS)
                .join(USERS).on(COMMENTS.USER_ID.eq(USERS.ID))
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .where(COMMENTS.POST_ID.eq(postId))
                .orderBy(COMMENTS.CREATED_AT.asc()) // Oldest comments first
                .fetchInto(ge.dola.talanti.feed.dto.CommentDto.class);
    }




}