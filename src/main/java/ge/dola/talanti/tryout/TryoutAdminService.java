package ge.dola.talanti.tryout;

import ge.dola.talanti.tryout.dto.TryoutApplicantDto;
import org.jooq.DSLContext;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Transactional(readOnly = true)
    @PreAuthorize("@clubAccessManager.authorize(authentication, #clubId).isGranted()")
    public List<TryoutApplicantDto> getApplicationsForClub(Long clubId, Long id) {
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
                // Binds the search strictly to the authorized club
                .where(TRYOUTS.CLUB_ID.eq(clubId))
                .fetch(record -> {
                    // ... your existing DTO mapping logic ...
                    return new TryoutApplicantDto(
                            record.get(TRYOUT_APPLICATIONS.ID),
                            record.get(USER_PROFILES.USER_ID),
                            record.get("name", String.class),
                            record.get(USER_PROFILES.PROFILE_PICTURE_URL),
                            record.get("ageGroup", String.class),
                            record.get(TRYOUT_APPLICATIONS.STATUS),
                            0, // matchScore
                            Map.of() // stats
                    );
                });
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.authorize(authentication, #clubId).isGranted()")
    public void updateApplicationStatus(Long clubId, Long applicationId, String status, Long adminId) {        // Security logic is handled by @PreAuthorize proxy.
        // Data Integrity is handled by the SQL constraint ensuring the application belongs to a tryout owned by the club.
        int updatedRows = dsl.update(TRYOUT_APPLICATIONS)
                .set(TRYOUT_APPLICATIONS.STATUS, status)
                .set(TRYOUT_APPLICATIONS.REVIEWED_AT, LocalDateTime.now())
                .set(TRYOUT_APPLICATIONS.REVIEWED_BY, adminId)
                .where(TRYOUT_APPLICATIONS.ID.eq(applicationId))
                .and(TRYOUT_APPLICATIONS.TRYOUT_ID.in(
                        dsl.select(TRYOUTS.ID).from(TRYOUTS).where(TRYOUTS.CLUB_ID.eq(clubId))
                ))
                .execute();

        if (updatedRows == 0) {
            throw new IllegalArgumentException("Application not found or does not belong to this club.");
        }
    }
}