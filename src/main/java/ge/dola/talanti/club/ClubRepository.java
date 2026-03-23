package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.ClubUpdateDto;
import ge.dola.talanti.jooq.tables.records.ClubsRecord;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.*;
import static ge.dola.talanti.jooq.tables.ClubFollows.CLUB_FOLLOWS;
import static ge.dola.talanti.jooq.tables.UserProfiles.USER_PROFILES;

@Repository
public class ClubRepository {

    private final DSLContext dsl;

    public ClubRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<ClubsRecord> findById(Long id) {
        return dsl.selectFrom(CLUBS)
                .where(CLUBS.ID.eq(id))
                .fetchOptional();
    }

    public List<ClubsRecord> findAll() {
        return dsl.selectFrom(CLUBS).fetch();
    }

    public ClubsRecord save(ClubsRecord club,
                            String contactEmail,
                            String whatsappNumber,
                            String facebookMessengerUrl,
                            String preferredCommunicationMethod) {
        return dsl.insertInto(CLUBS)
                .set(club)
                .set(CLUBS.CONTACT_EMAIL, contactEmail)
                .set(CLUBS.WHATSAPP_NUMBER, whatsappNumber)
                .set(ClubDynamicTables.CLUBS_FACEBOOK_MESSENGER_URL, facebookMessengerUrl)
                .set(ClubDynamicTables.CLUBS_PREFERRED_CONTACT_METHOD, preferredCommunicationMethod)
                .returning()
                .fetchOne();
    }

    public boolean existsByNormalizedName(String clubName) {
        String normalizedName = normalizeClubName(clubName);
        if (normalizedName == null) {
            return false;
        }

        return dsl.fetchExists(
                dsl.selectOne()
                        .from(CLUBS)
                        .where(normalizedNameField().eq(normalizedName))
        );
    }

    public boolean isUserFollowingClub(Long userId, Long clubId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(CLUB_FOLLOWS)
                        .where(CLUB_FOLLOWS.USER_ID.eq(userId))
                        .and(CLUB_FOLLOWS.CLUB_ID.eq(clubId))
        );
    }



    public void followClub(Long userId, Long clubId) {
        dsl.insertInto(CLUB_FOLLOWS)
                .set(CLUB_FOLLOWS.USER_ID, userId)
                .set(CLUB_FOLLOWS.CLUB_ID, clubId)
                .set(CLUB_FOLLOWS.CREATED_AT, LocalDateTime.now())
                .onDuplicateKeyIgnore() // Safety net: prevents crashing if they double-click fast
                .execute();
    }

    public void unfollowClub(Long userId, Long clubId) { // Pass userId down from service!
        dsl.deleteFrom(CLUB_FOLLOWS)
                .where(CLUB_FOLLOWS.CLUB_ID.eq(clubId))
                .and(CLUB_FOLLOWS.USER_ID.eq(userId)) // STRICT ENFORCEMENT
                .execute();
    }

    public void updateClubImages(Long clubId, ClubUpdateDto dto) {
        Map<Field<?>, Object> updates = new HashMap<>();

        if (dto.logoUrl() != null) {
            updates.put(CLUBS.LOGO_URL, dto.logoUrl());
        }

        if (dto.bannerUrl() != null) {
            updates.put(CLUBS.BANNER_URL, dto.bannerUrl());
        }

        // Only execute the query if there is actually something to update
        if (!updates.isEmpty()) {
            dsl.update(CLUBS)
                    .set(updates)
                    .where(CLUBS.ID.eq(clubId))
                    .execute();
        }
    }

    private static Field<String> normalizedNameField() {
        return DSL.field(
                "lower(regexp_replace(btrim({0}), '\\s+', ' ', 'g'))",
                String.class,
                CLUBS.NAME
        );
    }

    private static String normalizeClubName(String clubName) {
        if (clubName == null || clubName.isBlank()) {
            return null;
        }
        return clubName.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
