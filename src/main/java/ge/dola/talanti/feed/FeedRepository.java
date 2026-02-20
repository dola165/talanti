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

    /**
     * Fetches the feed for a specific user using Cursor-based pagination.
     * * @param currentUserId The user looking at the feed
     * @param cursor The ID of the last post they saw (null for the first page)
     * @param limit How many posts to return
     */
    public List<FeedPostDto> getFeedForUser(Long currentUserId, Long cursor, int limit) {

        var query = dsl.select(
                        POSTS.ID,
                        POSTS.CONTENT,
                        POSTS.CREATED_AT,
                        USERS.ID.as("authorId"),
                        USERS.USERNAME.as("authorName"),
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

                        // Subquery to check if current user liked it
                        DSL.field(
                                DSL.selectCount().from(LIKES)
                                        .where(LIKES.POST_ID.eq(POSTS.ID)
                                                .and(LIKES.USER_ID.eq(currentUserId)))
                        ).cast(Boolean.class).as("isLikedByMe")

                        // Note: To keep the MVP simple and avoid complex ARRAY_AGG casting,
                        // we will handle media fetching in a slightly different way if needed,
                        // or you can add a simple subquery returning a string array here.
                )
                .from(POSTS)
                .leftJoin(USERS).on(POSTS.AUTHOR_ID.eq(USERS.ID))
                .leftJoin(CLUBS).on(POSTS.CLUB_ID.eq(CLUBS.ID))
                .where(POSTS.IS_PUBLIC.eq(true))
                .and(
                        // The Feed Algorithm: Posts by me, OR people I follow, OR clubs I follow
                        POSTS.AUTHOR_ID.eq(currentUserId)
                                .or(POSTS.AUTHOR_ID.in(
                                        DSL.select(FOLLOWS.FOLLOWING_ID).from(FOLLOWS).where(FOLLOWS.FOLLOWER_ID.eq(currentUserId))
                                ))
                                .or(POSTS.CLUB_ID.in(
                                        DSL.select(CLUB_FOLLOWS.CLUB_ID).from(CLUB_FOLLOWS).where(CLUB_FOLLOWS.USER_ID.eq(currentUserId))
                                ))
                );

        // Apply cursor pagination
        if (cursor != null) {
            query.and(POSTS.ID.lessThan(cursor));
        }

        // Order by newest first, limit the results
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
                        List.of() // Empty list for MVP media. We can query media separately if needed.
                ));
    }
}