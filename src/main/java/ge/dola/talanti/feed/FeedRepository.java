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
                        // Fallback to username if profile name doesn't exist
                        DSL.coalesce(USER_PROFILES.FULL_NAME, USERS.USERNAME).as("authorName"),
                        CLUBS.ID.as("clubId"),
                        CLUBS.NAME.as("clubName"),

                        // Subquery for total likes
                        DSL.field(
                                DSL.selectCount().from(LIKES).where(LIKES.POST_ID.eq(POSTS.ID))
                        ).as("likeCount"),

                        // Subquery for total comments
                        DSL.field(
                                DSL.selectCount().from(COMMENTS).where(COMMENTS.POST_ID.eq(POSTS.ID))
                        ).as("commentCount"),

                        // FIX: Use DSL.exists() instead of casting count() to Boolean for PostgreSQL
                        DSL.field(
                                DSL.exists(
                                        DSL.selectOne().from(LIKES)
                                                .where(LIKES.POST_ID.eq(POSTS.ID)
                                                        .and(LIKES.USER_ID.eq(currentUserId)))
                                )
                        ).as("isLikedByMe")
                )
                .from(POSTS)
                .leftJoin(USERS).on(POSTS.AUTHOR_ID.eq(USERS.ID))
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .leftJoin(CLUBS).on(POSTS.CLUB_ID.eq(CLUBS.ID))
                // For the MVP, a Global Feed ordered by newest creates the best demo experience
                .where(POSTS.IS_PUBLIC.eq(true));

        if (cursor != null) {
            query.and(POSTS.ID.lessThan(cursor));
        }

        return query.orderBy(POSTS.ID.desc())
                .limit(limit)
                .fetch(record -> new FeedPostDto(
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
                        List.of() // Empty list for media placeholder
                ));
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
                        record.get("isLikedByMe", Boolean.class), List.of()
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
}