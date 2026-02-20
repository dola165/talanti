package ge.dola.talanti.dev;

import org.jooq.DSLContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static ge.dola.talanti.jooq.Tables.*;

@Component
// Optional: @Profile("dev") to ensure this never runs in production
public class DataSeeder {

    private final DSLContext dsl;

    public DataSeeder(DSLContext dsl) {
        this.dsl = dsl;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDatabase() {
        // Only seed if the users table is completely empty
        int userCount = dsl.fetchCount(USERS);
        if (userCount > 0) {
            System.out.println("Database already contains data. Skipping seeder.");
            return;
        }

        System.out.println("Seeding database with MVP dummy data...");

        // 1. Create a dummy user
        var player1 = dsl.insertInto(USERS)
                .set(USERS.EMAIL, "lionel@demo.com")
                .set(USERS.USERNAME, "lionel_m")
                .set(USERS.PASSWORD_HASH, "none")
                .set(USERS.CREATED_AT, LocalDateTime.now())
                .returning(USERS.ID).fetchOne();

        var player2 = dsl.insertInto(USERS)
                .set(USERS.EMAIL, "pep@demo.com")
                .set(USERS.USERNAME, "pep_coach")
                .set(USERS.PASSWORD_HASH, "none")
                .set(USERS.CREATED_AT, LocalDateTime.now())
                .returning(USERS.ID).fetchOne();

        // 2. Create their profiles
        dsl.insertInto(USER_PROFILES)
                .set(USER_PROFILES.USER_ID, player1.getId())
                .set(USER_PROFILES.FULL_NAME, "Lionel M.")
                .set(USER_PROFILES.POSITION, "Forward")
                .set(USER_PROFILES.PREFERRED_FOOT, "Left")
                .execute();

        // 3. Create a dummy club
        var club = dsl.insertInto(CLUBS)
                .set(CLUBS.NAME, "Tbilisi City FC")
                .set(CLUBS.DESCRIPTION, "Amateur club looking for new talent.")
                .set(CLUBS.TYPE, "Amateur")
                .set(CLUBS.IS_OFFICIAL, true)
                .set(CLUBS.CREATED_BY, player2.getId())
                .returning(CLUBS.ID).fetchOne();

        // 4. Create some posts for the feed
        dsl.insertInto(POSTS)
                .set(POSTS.AUTHOR_ID, player1.getId())
                .set(POSTS.CONTENT, "Just had a great training session! Looking for a club trial.")
                .set(POSTS.CREATED_AT, LocalDateTime.now().minusHours(2))
                .execute();

        dsl.insertInto(POSTS)
                .set(POSTS.AUTHOR_ID, player2.getId())
                .set(POSTS.CLUB_ID, club.getId())
                .set(POSTS.CONTENT, "Tbilisi City FC is holding open tryouts this Saturday. Message me for details!")
                .set(POSTS.CREATED_AT, LocalDateTime.now().minusHours(1))
                .execute();

        System.out.println("Database seeding complete.");
    }
}