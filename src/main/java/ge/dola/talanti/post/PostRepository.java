package ge.dola.talanti.post;

import ge.dola.talanti.feed.dto.CommentDto;
import ge.dola.talanti.jooq.tables.records.PostsRecord;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.CLUB_MEMBERSHIPS;
import static ge.dola.talanti.jooq.Tables.COMMENTS;
import static ge.dola.talanti.jooq.Tables.MEDIA;
import static ge.dola.talanti.jooq.Tables.POSTS;
import static ge.dola.talanti.jooq.Tables.POST_MEDIA;
import static ge.dola.talanti.jooq.Tables.USERS;
import static ge.dola.talanti.jooq.Tables.USER_PROFILES;
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

    public CommentDto addComment(Long postId, Long userId, String content) {
        Long commentId = dsl.insertInto(COMMENTS)
                .set(COMMENTS.POST_ID, postId)
                .set(COMMENTS.USER_ID, userId)
                .set(COMMENTS.CONTENT, content)
                .set(COMMENTS.CREATED_AT, LocalDateTime.now())
                .returningResult(COMMENTS.ID)
                .fetchOneInto(Long.class);

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
                .where(COMMENTS.ID.eq(commentId))
                .fetchOneInto(CommentDto.class);
    }

    public boolean areAllMediaOwnedByUser(Long userId, List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return true;
        }

        List<Long> uniqueMediaIds = mediaIds.stream().distinct().toList();
        Integer ownedCount = dsl.selectCount()
                .from(MEDIA)
                .where(MEDIA.ID.in(uniqueMediaIds))
                .and(MEDIA.UPLOADED_BY.eq(userId))
                .fetchOne(0, Integer.class);

        return ownedCount != null && ownedCount == uniqueMediaIds.size();
    }

    public void linkMediaToPost(Long postId, List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return;
        }

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
                .onDuplicateKeyIgnore()
                .execute();
    }

    public void unlikePost(Long postId, Long userId) {
        dsl.deleteFrom(LIKES)
                .where(LIKES.POST_ID.eq(postId))
                .and(LIKES.USER_ID.eq(userId))
                .execute();
    }

    public boolean postExists(Long postId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(POSTS)
                        .where(POSTS.ID.eq(postId))
        );
    }

    public boolean isPublicPost(Long postId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(POSTS)
                        .where(POSTS.ID.eq(postId))
                        .and(POSTS.IS_PUBLIC.eq(true))
        );
    }

    public boolean canUserAccessPost(Long postId, Long userId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(POSTS)
                        .where(POSTS.ID.eq(postId))
                        .and(
                                POSTS.IS_PUBLIC.eq(true)
                                        .or(POSTS.AUTHOR_ID.eq(userId))
                                        .or(DSL.exists(
                                                DSL.selectOne()
                                                        .from(CLUB_MEMBERSHIPS)
                                                        .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(POSTS.CLUB_ID))
                                                        .and(CLUB_MEMBERSHIPS.USER_ID.eq(userId))
                                                        .and(CLUB_MEMBERSHIPS.ROLE.in("OWNER", "CLUB_ADMIN"))
                                        ))
                        )
        );
    }

    public boolean isUserAdminOfClub(Long userId, Long clubId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(CLUB_MEMBERSHIPS)
                        .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(clubId))
                        .and(CLUB_MEMBERSHIPS.USER_ID.eq(userId))
                        .and(CLUB_MEMBERSHIPS.ROLE.in("OWNER", "CLUB_ADMIN"))
        );
    }

    public Optional<PostsRecord> findById(Long postId) {
        return dsl.selectFrom(POSTS)
                .where(POSTS.ID.eq(postId))
                .fetchOptional();
    }

    public void delete(Long postId) {
        dsl.deleteFrom(POSTS)
                .where(POSTS.ID.eq(postId))
                .execute();
    }
}
