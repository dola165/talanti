package ge.dola.talanti.feed;

import ge.dola.talanti.feed.dto.FeedPostDto;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;

import static ge.dola.talanti.jooq.Tables.*;

@Repository
public class FeedRepository {

    private final DSLContext dsl;

    public FeedRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

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

    // --- LIKES ---
    public boolean toggleLike(Long postId, Long userId) {
        boolean exists = dsl.fetchExists(
                dsl.selectOne().from(LIKES)
                        .where(LIKES.POST_ID.eq(postId).and(LIKES.USER_ID.eq(userId)))
        );

        if (exists) {
            dsl.deleteFrom(LIKES).where(LIKES.POST_ID.eq(postId).and(LIKES.USER_ID.eq(userId))).execute();
            return false; // No longer liked
        } else {
            dsl.insertInto(LIKES)
                    .set(LIKES.POST_ID, postId)
                    .set(LIKES.USER_ID, userId)
                    .set(LIKES.CREATED_AT, java.time.LocalDateTime.now())
                    .execute();
            return true; // Now liked
        }
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

    public ge.dola.talanti.feed.dto.CommentDto addComment(Long postId, Long userId, String content) {
        Long commentId = dsl.insertInto(COMMENTS)
                .set(COMMENTS.POST_ID, postId)
                .set(COMMENTS.USER_ID, userId)
                .set(COMMENTS.CONTENT, content)
                .set(COMMENTS.CREATED_AT, java.time.LocalDateTime.now())
                .returningResult(COMMENTS.ID)
                .fetchOneInto(Long.class);

        // Fetch it back immediately to return the full DTO (with the author's name) to React
        return dsl.select(
                        COMMENTS.ID,
                        DSL.coalesce(USER_PROFILES.FULL_NAME, USERS.USERNAME).as("authorName"),
                        COMMENTS.CONTENT,
                        COMMENTS.CREATED_AT
                )
                .from(COMMENTS)
                .join(USERS).on(COMMENTS.USER_ID.eq(USERS.ID))
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .where(COMMENTS.ID.eq(commentId))
                .fetchOneInto(ge.dola.talanti.feed.dto.CommentDto.class);
    }


    @org.springframework.transaction.annotation.Transactional
    public void createPost(Long authorId, ge.dola.talanti.feed.dto.CreatePostRequest request) {
        // 1. Create the Post
        Long postId = dsl.insertInto(POSTS)
                .set(POSTS.AUTHOR_ID, authorId)
                .set(POSTS.CLUB_ID, request.clubId())
                .set(POSTS.CONTENT, request.content())
                .set(POSTS.IS_PUBLIC, true)
                .set(POSTS.CREATED_AT, java.time.LocalDateTime.now())
                .returningResult(POSTS.ID)
                .fetchOneInto(Long.class);

        // 2. Link the Uploaded Media
        if (request.mediaIds() != null && !request.mediaIds().isEmpty()) {
            var mediaInsert = dsl.insertInto(POST_MEDIA, POST_MEDIA.POST_ID, POST_MEDIA.MEDIA_ID, POST_MEDIA.DISPLAY_ORDER);
            for (int i = 0; i < request.mediaIds().size(); i++) {
                mediaInsert = mediaInsert.values(postId, request.mediaIds().get(i), i);
            }
            mediaInsert.execute();
        }

        // 3. Link the Tagged Players (For their Match Feed)
        if (request.taggedUserIds() != null && !request.taggedUserIds().isEmpty()) {
            var tagsInsert = dsl.insertInto(POST_TAGS, POST_TAGS.POST_ID, POST_TAGS.USER_ID);
            for (Long taggedUserId : request.taggedUserIds()) {
                tagsInsert = tagsInsert.values(postId, taggedUserId);
            }
            tagsInsert.execute();
        }
    }
}