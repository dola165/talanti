package ge.dola.talanti.config;

import ge.dola.talanti.club.ClubRole;
import ge.dola.talanti.user.UserType;
import org.jooq.DSLContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static ge.dola.talanti.jooq.Tables.*;

@Component
public class DataSeeder implements CommandLineRunner {

    private final DSLContext dsl;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(DSLContext dsl, PasswordEncoder passwordEncoder) {
        this.dsl = dsl;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (dsl.fetchCount(dsl.selectFrom(USERS)) > 0) {
            System.out.println("🌱 Database already populated. Skipping seeder.");
            return;
        }

        System.out.println("🌱 Starting Talanti Master Seeder...");
        String commonPasswordHash = passwordEncoder.encode("password");

        // 1. Create the Master Coach Persona (Using the strict Enum)
        Long coachId = dsl.insertInto(USERS)
                .set(USERS.USERNAME, "coach_luka")
                .set(USERS.EMAIL, "coach@talanti.ge")
                .set(USERS.PASSWORD_HASH, commonPasswordHash)
                .set(USERS.USER_TYPE, UserType.CLUB_ADMIN.name()) // STRICT ENFORCEMENT
                .set(USERS.CREATED_AT, LocalDateTime.now())
                .returningResult(USERS.ID)
                .fetchOneInto(Long.class);

        dsl.insertInto(USER_PROFILES)
                .set(USER_PROFILES.USER_ID, coachId)
                .set(USER_PROFILES.FULL_NAME, "Coach Luka")
                .set(USER_PROFILES.BIO, "UEFA A License. Developing the next generation of Georgian talent.")
                .execute();

        // 2. Create a Location (Using the updated map logic)
        Long locationId = dsl.insertInto(LOCATIONS)
                .set(LOCATIONS.LATITUDE, BigDecimal.valueOf(41.7151))
                .set(LOCATIONS.LONGITUDE, BigDecimal.valueOf(44.8271))
                .set(LOCATIONS.ADDRESS_TEXT, "Boris Paichadze Dinamo Arena, Tbilisi")
                .set(LOCATIONS.CREATED_AT, LocalDateTime.now())
                .returningResult(LOCATIONS.ID)
                .fetchOneInto(Long.class);

        // 3. Create the Club
        Long dinamoId = dsl.insertInto(CLUBS)
                .set(CLUBS.NAME, "FC Dinamo Tbilisi Academy")
                .set(CLUBS.TYPE, "ACADEMY")
                .set(CLUBS.STATUS, "VERIFIED")
                .set(CLUBS.LOCATION_ID, locationId)
                .set(CLUBS.CREATED_BY, coachId)
                .set(CLUBS.CREATED_AT, LocalDateTime.now())
                .returningResult(CLUBS.ID)
                .fetchOneInto(Long.class);

        // 4. Assign Ownership (Using the strict ClubRole Enum)
        dsl.insertInto(CLUB_MEMBERSHIPS)
                .set(CLUB_MEMBERSHIPS.CLUB_ID, dinamoId)
                .set(CLUB_MEMBERSHIPS.USER_ID, coachId)
                .set(CLUB_MEMBERSHIPS.ROLE, ClubRole.OWNER.name()) // STRICT ENFORCEMENT
                .set(CLUB_MEMBERSHIPS.JOINED_AT, LocalDateTime.now())
                .execute();

        // 5. Create a Squad for the club (V2 Architecture Update)
        Long squadId = dsl.insertInto(SQUADS)
                .set(SQUADS.CLUB_ID, dinamoId)
                .set(SQUADS.NAME, "U16 Boys")
                .set(SQUADS.CATEGORY, "U16")
                .set(SQUADS.GENDER, "MALE")
                .set(SQUADS.HEAD_COACH_ID, coachId)
                .set(SQUADS.CREATED_AT, LocalDateTime.now())
                .returningResult(SQUADS.ID)
                .fetchOneInto(Long.class);

        // 6. Seed a public post
        dsl.insertInto(POSTS)
                .set(POSTS.AUTHOR_ID, coachId)
                .set(POSTS.CLUB_ID, dinamoId)
                .set(POSTS.CONTENT, "We are officially launching our new U16 scouting program! Check the Map to find our next open tryout.")
                .set(POSTS.IS_PUBLIC, true)
                .set(POSTS.CREATED_AT, LocalDateTime.now())
                .execute();

        // 7. Seed an Opportunity (V3 Architecture Update - requires external_link)
        dsl.insertInto(CLUB_OPPORTUNITIES)
                .set(CLUB_OPPORTUNITIES.CLUB_ID, dinamoId)
                .set(CLUB_OPPORTUNITIES.TYPE, "FUNDRAISING")
                .set(CLUB_OPPORTUNITIES.TITLE, "New Training Equipment Fund")
                .set(CLUB_OPPORTUNITIES.DESCRIPTION, "Help us buy new GPS vests for the U16 squad.")
                .set(CLUB_OPPORTUNITIES.EXTERNAL_LINK, "https://gofundme.com/example")
                .set(CLUB_OPPORTUNITIES.STATUS, "OPEN")
                .set(CLUB_OPPORTUNITIES.CREATED_AT, LocalDateTime.now())
                .execute();

        System.out.println("✅ Talanti Master Seeder Complete.");
    }
}