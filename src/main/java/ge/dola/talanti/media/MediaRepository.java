package ge.dola.talanti.media;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static ge.dola.talanti.jooq.Tables.MEDIA;

@Repository
@RequiredArgsConstructor
public class MediaRepository {

    private final DSLContext dsl;

    public Long saveMedia(String url, String type, long sizeBytes, Long uploaderId) {
        return dsl.insertInto(MEDIA)
                .set(MEDIA.URL, url)
                .set(MEDIA.TYPE, type)
                .set(MEDIA.SIZE_BYTES, sizeBytes)
                .set(MEDIA.UPLOADED_BY, uploaderId)
                .set(MEDIA.CREATED_AT, LocalDateTime.now())
                .returningResult(MEDIA.ID)
                .fetchOneInto(Long.class);
    }
}