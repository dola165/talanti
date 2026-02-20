package ge.dola.talanti.post;

import ge.dola.talanti.jooq.tables.records.PostsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static ge.dola.talanti.jooq.Tables.POSTS;

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
}