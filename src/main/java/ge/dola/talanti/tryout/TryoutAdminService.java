package ge.dola.talanti.tryout;

import ge.dola.talanti.tryout.dto.TryoutApplicantDto;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static ge.dola.talanti.jooq.Tables.*;

@Service
public class TryoutAdminService {

    private final DSLContext dsl;

    public TryoutAdminService(DSLContext dsl) {
        this.dsl = dsl;
    }

    // NEW: Fetch applications specifically for a club the admin manages
    public List<TryoutApplicantDto> getApplicationsForClub(Long clubId, Long adminId) {
        return dsl.select(
                        TRYOUT_APPLICATIONS.ID,
                        USER_PROFILES.USER_ID,
                        USER_PROFILES.FULL_NAME.as("name"),
                        USER_PROFILES.POSITION,
                        TRYOUTS.AGE_GROUP.as("ageGroup"),
                        TRYOUT_APPLICATIONS.STATUS
                )
                .from(TRYOUT_APPLICATIONS)
                .join(TRYOUTS).on(TRYOUT_APPLICATIONS.TRYOUT_ID.eq(TRYOUTS.ID))
                .join(USER_PROFILES).on(TRYOUT_APPLICATIONS.USER_ID.eq(USER_PROFILES.USER_ID))
                .where(TRYOUTS.CLUB_ID.eq(clubId))
                .and(TRYOUTS.CREATED_BY.eq(adminId)) // Security Check
                .orderBy(TRYOUT_APPLICATIONS.APPLIED_AT.desc())
                .fetch(record -> {
                    long uid = record.get(USER_PROFILES.USER_ID);
                    // Generate pseudo-random but consistent stats for the UI demo
                    int matchScore = (int) (75 + (uid % 20));
                    int pace = (int) (70 + (uid % 25));
                    int passing = (int) (65 + (uid % 30));
                    int phys = (int) (70 + (uid % 20));

                    return new TryoutApplicantDto(
                            record.get(TRYOUT_APPLICATIONS.ID),
                            uid,
                            record.get("name", String.class) != null ? record.get("name", String.class) : "Unknown Player",
                            record.get(USER_PROFILES.POSITION) != null ? record.get(USER_PROFILES.POSITION) : "Any",
                            record.get("ageGroup", String.class),
                            record.get(TRYOUT_APPLICATIONS.STATUS),
                            matchScore,
                            Map.of("pace", pace, "passing", passing, "physicality", phys)
                    );
                });
    }

    @Transactional
    public void updateApplicationStatus(Long applicationId, String status, Long adminId) {
        boolean isOwner = dsl.fetchExists(
                dsl.selectOne().from(TRYOUT_APPLICATIONS)
                        .join(TRYOUTS).on(TRYOUT_APPLICATIONS.TRYOUT_ID.eq(TRYOUTS.ID))
                        .where(TRYOUT_APPLICATIONS.ID.eq(applicationId))
                        .and(TRYOUTS.CREATED_BY.eq(adminId))
        );

        if (!isOwner) {
            throw new RuntimeException("ACCESS DENIED: You do not own this tryout.");
        }

        dsl.update(TRYOUT_APPLICATIONS)
                .set(TRYOUT_APPLICATIONS.STATUS, status)
                .set(TRYOUT_APPLICATIONS.REVIEWED_AT, LocalDateTime.now())
                .set(TRYOUT_APPLICATIONS.REVIEWED_BY, adminId)
                .where(TRYOUT_APPLICATIONS.ID.eq(applicationId))
                .execute();
    }
}