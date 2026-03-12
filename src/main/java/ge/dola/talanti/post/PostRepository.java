package ge.dola.talanti.post;

import ge.dola.talanti.jooq.tables.records.PostsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.*;
import static ge.dola.talanti.jooq.tables.Likes.LIKES;

@Repository
public class PostRepository {

    private final DSLContext dsl;

    public PostRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public PostsRecord createPost(Long authorId, String content, Long clubId, Boolean isPublic) {
        return dsl.insertInto(POSTS)
                .set(POSTS.AUTHOR_ID, authorId)
                .set(POSTS.CONTENT, content)
                .set(POSTS.CLUB_ID, clubId)
                .set(POSTS.IS_PUBLIC, isPublic != null ? isPublic : true)
                .set(POSTS.CREATED_AT, LocalDateTime.now())
                .returning()
                .fetchOne();
    }

    // NEW: Batch insert the media links
    public void linkMediaToPost(Long postId, List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) return;

        var insertStep = dsl.insertInto(POST_MEDIA, POST_MEDIA.POST_ID, POST_MEDIA.MEDIA_ID, POST_MEDIA.DISPLAY_ORDER);

        int order = 0;
        for (Long mediaId : mediaIds) {
            insertStep = insertStep.values(postId, mediaId, order++);
        }

        insertStep.execute();
    }

    public boolean isPostLikedByUser(Long postId, Long userId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(LIKES)
                        .where(LIKES.POST_ID.eq(postId))
                        .and(LIKES.USER_ID.eq(userId))
        );
    }

    public void likePost(Long postId, Long userId) {
        dsl.insertInto(LIKES)
                .set(LIKES.POST_ID, postId)
                .set(LIKES.USER_ID, userId)
                .set(LIKES.CREATED_AT, LocalDateTime.now())
                .onDuplicateKeyIgnore() // Safety net for double-clicks
                .execute();
    }

    public void unlikePost(Long postId, Long userId) {
        dsl.deleteFrom(LIKES)
                .where(LIKES.POST_ID.eq(postId))
                .and(LIKES.USER_ID.eq(userId))
                .execute();
    }

    // Quick helper to check if a post actually exists before we try to like it
    public boolean postExists(Long postId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(POSTS)
                        .where(POSTS.ID.eq(postId))
        );
    }


    public boolean isUserAdminOfClub(Long userId, Long clubId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(CLUB_MEMBERSHIPS)
                        .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(clubId))
                        .and(CLUB_MEMBERSHIPS.USER_ID.eq(userId))
                        // Note: Adjust "ADMIN" to match whatever exact string you use for roles
                        .and(CLUB_MEMBERSHIPS.ROLE.eq("ADMIN"))
        );
    }

    /**
     * Fetches a post record safely wrapped in an Optional.
     */
    public Optional<PostsRecord> findById(Long postId) {
        return dsl.selectFrom(POSTS)
                .where(POSTS.ID.eq(postId))
                .fetchOptional();
    }

    /**
     * Deletes a post from the database.
     */
    public void delete(Long postId) {
        dsl.deleteFrom(POSTS)
                .where(POSTS.ID.eq(postId))
                .execute();
    }
}