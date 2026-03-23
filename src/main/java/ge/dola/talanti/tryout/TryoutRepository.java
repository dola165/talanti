package ge.dola.talanti.tryout;

import ge.dola.talanti.tryout.dto.TryoutApplicantDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.CLUBS;
import static ge.dola.talanti.jooq.Tables.TRYOUTS;
import static ge.dola.talanti.jooq.Tables.TRYOUT_APPLICATIONS;
import static ge.dola.talanti.jooq.Tables.USER_PROFILES;

@Repository
@RequiredArgsConstructor
public class TryoutRepository {

    private final DSLContext dsl;

    public Optional<TryoutSummary> findTryoutSummary(Long tryoutId) {
        return dsl.select(
                        TRYOUTS.ID,
                        TRYOUTS.CLUB_ID,
                        TRYOUTS.TITLE,
                        CLUBS.NAME.as("clubName")
                )
                .from(TRYOUTS)
                .join(CLUBS).on(CLUBS.ID.eq(TRYOUTS.CLUB_ID))
                .where(TRYOUTS.ID.eq(tryoutId))
                .fetchOptional(record -> new TryoutSummary(
                        record.get(TRYOUTS.ID),
                        record.get(TRYOUTS.CLUB_ID),
                        record.get(TRYOUTS.TITLE),
                        record.get("clubName", String.class)
                ));
    }

    public boolean hasUserApplied(Long tryoutId, Long userId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(TRYOUT_APPLICATIONS)
                        .where(TRYOUT_APPLICATIONS.TRYOUT_ID.eq(tryoutId))
                        .and(TRYOUT_APPLICATIONS.USER_ID.eq(userId))
        );
    }

    public Long createApplication(Long tryoutId, Long userId, String message) {
        return dsl.insertInto(TRYOUT_APPLICATIONS)
                .set(TRYOUT_APPLICATIONS.TRYOUT_ID, tryoutId)
                .set(TRYOUT_APPLICATIONS.USER_ID, userId)
                .set(TRYOUT_APPLICATIONS.STATUS, "PENDING")
                .set(TRYOUT_APPLICATIONS.MESSAGE, message)
                .set(TRYOUT_APPLICATIONS.APPLIED_AT, LocalDateTime.now())
                .returningResult(TRYOUT_APPLICATIONS.ID)
                .fetchOneInto(Long.class);
    }

    public List<TryoutApplicantDto> getApplicationsForClub(Long clubId) {
        return dsl.select(
                        TRYOUT_APPLICATIONS.ID,
                        USER_PROFILES.USER_ID,
                        USER_PROFILES.FULL_NAME.as("name"),
                        USER_PROFILES.PROFILE_PICTURE_URL,
                        TRYOUTS.AGE_GROUP.as("ageGroup"),
                        TRYOUT_APPLICATIONS.STATUS
                )
                .from(TRYOUT_APPLICATIONS)
                .join(TRYOUTS).on(TRYOUT_APPLICATIONS.TRYOUT_ID.eq(TRYOUTS.ID))
                .join(USER_PROFILES).on(TRYOUT_APPLICATIONS.USER_ID.eq(USER_PROFILES.USER_ID))
                .where(TRYOUTS.CLUB_ID.eq(clubId))
                .orderBy(TRYOUT_APPLICATIONS.APPLIED_AT.desc(), TRYOUT_APPLICATIONS.ID.desc())
                .fetch(record -> new TryoutApplicantDto(
                        record.get(TRYOUT_APPLICATIONS.ID),
                        record.get(USER_PROFILES.USER_ID),
                        record.get("name", String.class),
                        record.get(USER_PROFILES.PROFILE_PICTURE_URL),
                        record.get("ageGroup", String.class),
                        record.get(TRYOUT_APPLICATIONS.STATUS),
                        0,
                        Map.of()
                ));
    }

    public Optional<TryoutApplicationContext> findApplicationContext(Long clubId, Long applicationId) {
        return dsl.select(
                        TRYOUT_APPLICATIONS.ID,
                        TRYOUT_APPLICATIONS.USER_ID,
                        TRYOUT_APPLICATIONS.STATUS,
                        TRYOUTS.ID.as("tryoutId"),
                        TRYOUTS.TITLE.as("tryoutTitle"),
                        TRYOUTS.CLUB_ID,
                        CLUBS.NAME.as("clubName")
                )
                .from(TRYOUT_APPLICATIONS)
                .join(TRYOUTS).on(TRYOUT_APPLICATIONS.TRYOUT_ID.eq(TRYOUTS.ID))
                .join(CLUBS).on(CLUBS.ID.eq(TRYOUTS.CLUB_ID))
                .where(TRYOUT_APPLICATIONS.ID.eq(applicationId))
                .and(TRYOUTS.CLUB_ID.eq(clubId))
                .fetchOptional(record -> new TryoutApplicationContext(
                        record.get(TRYOUT_APPLICATIONS.ID),
                        record.get(TRYOUT_APPLICATIONS.USER_ID),
                        record.get("tryoutId", Long.class),
                        record.get(TRYOUTS.CLUB_ID),
                        record.get("tryoutTitle", String.class),
                        record.get("clubName", String.class),
                        record.get(TRYOUT_APPLICATIONS.STATUS)
                ));
    }

    public int updateApplicationStatus(Long clubId, Long applicationId, String status, Long reviewerId, LocalDateTime reviewedAt) {
        return dsl.update(TRYOUT_APPLICATIONS)
                .set(TRYOUT_APPLICATIONS.STATUS, status)
                .set(TRYOUT_APPLICATIONS.REVIEWED_AT, reviewedAt)
                .set(TRYOUT_APPLICATIONS.REVIEWED_BY, reviewerId)
                .where(TRYOUT_APPLICATIONS.ID.eq(applicationId))
                .and(TRYOUT_APPLICATIONS.TRYOUT_ID.in(
                        dsl.select(TRYOUTS.ID).from(TRYOUTS).where(TRYOUTS.CLUB_ID.eq(clubId))
                ))
                .execute();
    }

    public record TryoutSummary(Long id, Long clubId, String title, String clubName) {
    }

    public record TryoutApplicationContext(
            Long applicationId,
            Long applicantUserId,
            Long tryoutId,
            Long clubId,
            String tryoutTitle,
            String clubName,
            String status
    ) {
    }
}
