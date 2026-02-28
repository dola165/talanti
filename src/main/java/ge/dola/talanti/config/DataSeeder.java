package ge.dola.talanti.config;

import net.datafaker.Faker;
import org.jooq.DSLContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static ge.dola.talanti.jooq.Tables.*;

@Component
public class DataSeeder {

    private final DSLContext dsl;

    public DataSeeder(DSLContext dsl) {
        this.dsl = dsl;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDatabase() {
        // 1. Check if we already have data to prevent infinite duplicates on restart
        int userCount = dsl.fetchCount(dsl.selectFrom(USERS));
        if (userCount > 5) {
            System.out.println("Database already populated. Skipping massive seeder.");
            return;
        }

        System.out.println("Starting Massive Data Seeder with Datafaker...");
        Faker faker = new Faker();
        Random random = new Random();

        // Keep track of IDs so we can link them later
        List<Long> userIds = new ArrayList<>();
        List<Long> clubIds = new ArrayList<>();
        List<Long> postIds = new ArrayList<>();

        // --- 1. GENERATE 20 USERS & PROFILES ---
        System.out.println("Generating Users...");
        String[] positions = {"Forward", "Midfielder", "Defender", "Goalkeeper"};
        String[] feet = {"Right", "Left", "Both"};

        for (int i = 0; i < 20; i++) {
            String username = faker.internet().username();

            // Insert User
            Long userId = dsl.insertInto(USERS)
                    .set(USERS.EMAIL, faker.internet().emailAddress())
                    .set(USERS.USERNAME, username)
                    .set(USERS.PASSWORD_HASH, "none") // MVP bypass
                    .set(USERS.CREATED_AT, LocalDateTime.now().minusDays(random.nextInt(30)))
                    .returningResult(USERS.ID)
                    .fetchOneInto(Long.class);

            userIds.add(userId);

            // Insert matching Profile
            dsl.insertInto(USER_PROFILES)
                    .set(USER_PROFILES.USER_ID, userId)
                    .set(USER_PROFILES.FULL_NAME, faker.name().fullName())
                    .set(USER_PROFILES.POSITION, positions[random.nextInt(positions.length)])
                    .set(USER_PROFILES.PREFERRED_FOOT, feet[random.nextInt(feet.length)])
                    .set(USER_PROFILES.BIO, faker.lorem().sentence(10))
                    .execute();
        }

        // --- 2. GENERATE 5 CLUBS & LOCATIONS ---
        System.out.println("Generating Clubs and Locations...");
        String[] clubTypes = {"Amateur", "Semi-Pro", "Academy"};

        // Create a bounding box roughly around Tbilisi for the fakes
        double baseLat = 41.7151;
        double baseLng = 44.8271;

        for (int i = 0; i < 5; i++) {
            // 2a. Insert the Club
            Long clubId = dsl.insertInto(CLUBS)
                    .set(CLUBS.NAME, faker.address().city() + " " + faker.team().creature())
                    .set(CLUBS.DESCRIPTION, faker.company().catchPhrase() + ". " + faker.lorem().paragraph())
                    .set(CLUBS.TYPE, clubTypes[random.nextInt(clubTypes.length)])
                    .set(CLUBS.IS_OFFICIAL, random.nextBoolean())
                    .set(CLUBS.CREATED_BY, userIds.get(0)) // Just assign to the first user
                    .returningResult(CLUBS.ID)
                    .fetchOneInto(Long.class);

            clubIds.add(clubId);

            // 2b. Give the fake club a location in Tbilisi
            double randomLat = baseLat + (random.nextDouble() * 0.1 - 0.05);
            double randomLng = baseLng + (random.nextDouble() * 0.1 - 0.05);

            dsl.insertInto(LOCATIONS)
                    .set(LOCATIONS.ENTITY_TYPE, "CLUB")
                    .set(LOCATIONS.ENTITY_ID, clubId)
                    .set(LOCATIONS.LATITUDE, BigDecimal.valueOf(randomLat))
                    .set(LOCATIONS.LONGITUDE, BigDecimal.valueOf(randomLng))
                    .set(LOCATIONS.ADDRESS_TEXT, faker.address().streetAddress())
                    .set(LOCATIONS.CREATED_AT, LocalDateTime.now())
                    .execute();

            // 2c. Create a guaranteed "Tryout" announcement post for this club
            dsl.insertInto(POSTS)
                    .set(POSTS.AUTHOR_ID, userIds.get(0)) // Posted by the club admin
                    .set(POSTS.CLUB_ID, clubId)
                    .set(POSTS.CONTENT, "🚨 OPEN TRYOUTS! 🚨\nWe are looking for new talent to join our squad. " +
                            "Trials will be held this Saturday at our home ground. Message us to register!")
                    .set(POSTS.CREATED_AT, LocalDateTime.now())
                    .execute();
        }

        // --- 3. GENERATE 50 POSTS ---
        System.out.println("Generating Posts...");
        for (int i = 0; i < 50; i++) {
            Long authorId = userIds.get(random.nextInt(userIds.size()));

            // 20% chance the post belongs to a club
            Long clubId = null;
            if (random.nextInt(100) < 20) {
                clubId = clubIds.get(random.nextInt(clubIds.size()));
            }

            Long postId = dsl.insertInto(POSTS)
                    .set(POSTS.AUTHOR_ID, authorId)
                    .set(POSTS.CLUB_ID, clubId)
                    .set(POSTS.CONTENT, faker.lorem().paragraph(random.nextInt(3) + 1))
                    .set(POSTS.CREATED_AT, LocalDateTime.now().minusHours(random.nextInt(200)))
                    .returningResult(POSTS.ID)
                    .fetchOneInto(Long.class);

            postIds.add(postId);
        }

        // --- 4. GENERATE RANDOM LIKES, FOLLOWS & MEMBERSHIPS ---
        System.out.println("Simulating Social Interactions...");
        for (Long userId : userIds) {
            // User likes 5 random posts
            for (int i = 0; i < 5; i++) {
                dsl.insertInto(LIKES)
                        .set(LIKES.USER_ID, userId)
                        .set(LIKES.POST_ID, postIds.get(random.nextInt(postIds.size())))
                        .set(LIKES.CREATED_AT, LocalDateTime.now())
                        .onDuplicateKeyIgnore()
                        .execute();
            }

            // User follows 2 random clubs
            for (int i = 0; i < 2; i++) {
                dsl.insertInto(CLUB_FOLLOWS)
                        .set(CLUB_FOLLOWS.USER_ID, userId)
                        .set(CLUB_FOLLOWS.CLUB_ID, clubIds.get(random.nextInt(clubIds.size())))
                        .set(CLUB_FOLLOWS.CREATED_AT, LocalDateTime.now())
                        .onDuplicateKeyIgnore()
                        .execute();
            }

            // User joins 1 random club as a Squad Member
            dsl.insertInto(CLUB_MEMBERSHIPS)
                    .set(CLUB_MEMBERSHIPS.USER_ID, userId)
                    .set(CLUB_MEMBERSHIPS.CLUB_ID, clubIds.get(random.nextInt(clubIds.size())))
                    .set(CLUB_MEMBERSHIPS.ROLE, "Player") // <--- FIX APPLIED HERE
                    .set(CLUB_MEMBERSHIPS.JOINED_AT, LocalDateTime.now())
                    .onDuplicateKeyIgnore()
                    .execute();

            // NEW: User comments on 3 random posts
            for (int i = 0; i < 3; i++) {
                dsl.insertInto(COMMENTS)
                        .set(COMMENTS.USER_ID, userId)
                        .set(COMMENTS.POST_ID, postIds.get(random.nextInt(postIds.size())))
                        .set(COMMENTS.CONTENT, faker.lorem().sentence(random.nextInt(5) + 3))
                        .set(COMMENTS.CREATED_AT, LocalDateTime.now().minusMinutes(random.nextInt(1000)))
                        .execute();
            }
        }

        // Always ensure your specific Dev Login user exists so you can test easily
        dsl.insertInto(USERS)
                .set(USERS.EMAIL, "react_dev@demo.com")
                .set(USERS.USERNAME, "react_dev")
                .set(USERS.PASSWORD_HASH, "dev_bypass")
                .set(USERS.CREATED_AT, LocalDateTime.now())
                .onDuplicateKeyIgnore()
                .execute();

        System.out.println("Massive Data Seeding Complete! MVP is fully populated.");
    }
}