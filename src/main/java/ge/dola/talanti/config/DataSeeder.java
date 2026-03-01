package ge.dola.talanti.config;

import net.datafaker.Faker;
import org.jooq.DSLContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static ge.dola.talanti.jooq.Tables.*;

@Component
public class DataSeeder implements CommandLineRunner {

    private final DSLContext dsl;

    public DataSeeder(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int userCount = dsl.fetchCount(dsl.selectFrom(USERS));
        if (userCount > 5) {
            System.out.println("🌱 Database already populated. Skipping massive seeder.");
            return;
        }

        System.out.println("🌱 Starting Massive Data Seeder with Datafaker...");
        Faker faker = new Faker();
        Random random = new Random();

        List<Long> userIds = new ArrayList<>();
        List<Long> clubIds = new ArrayList<>();
        List<Long> postIds = new ArrayList<>();

        // --- 1. GENERATE 100 USERS ---
        String[] positions = {"Forward", "Midfielder", "Defender", "Goalkeeper", "Striker", "Winger", "Center Back", "Full Back"};
        String[] feet = {"Right", "Left", "Both"};

        for (int i = 0; i < 100; i++) {
            Long userId = dsl.insertInto(USERS)
                    .set(USERS.EMAIL, faker.internet().emailAddress())
                    .set(USERS.USERNAME, faker.internet().username())
                    .set(USERS.PASSWORD_HASH, "none")
                    .set(USERS.CREATED_AT, LocalDateTime.now().minusDays(random.nextInt(30)))
                    .returningResult(USERS.ID)
                    .fetchOneInto(Long.class);
            userIds.add(userId);

            dsl.insertInto(USER_PROFILES)
                    .set(USER_PROFILES.USER_ID, userId)
                    .set(USER_PROFILES.FULL_NAME, faker.name().fullName())
                    .set(USER_PROFILES.POSITION, positions[random.nextInt(positions.length)])
                    .set(USER_PROFILES.PREFERRED_FOOT, feet[random.nextInt(feet.length)])
                    .set(USER_PROFILES.BIO, faker.lorem().paragraph(2))
                    .execute();
        }

        // --- 2. GENERATE 30 CLUBS & TRYOUTS ---
        String[] clubTypes = {"Amateur", "Semi-Pro", "Academy", "Professional"};
        double baseLat = 41.7151; // Tbilisi Center
        double baseLng = 44.8271;

        for (int i = 0; i < 30; i++) {
            Long clubId = dsl.insertInto(CLUBS)
                    .set(CLUBS.NAME, faker.address().city() + " " + faker.team().creature())
                    .set(CLUBS.DESCRIPTION, faker.company().catchPhrase() + ". " + faker.lorem().paragraph())
                    .set(CLUBS.TYPE, clubTypes[random.nextInt(clubTypes.length)])
                    .set(CLUBS.IS_OFFICIAL, random.nextBoolean())
                    .set(CLUBS.CREATED_BY, userIds.get(random.nextInt(userIds.size())))
                    .returningResult(CLUBS.ID)
                    .fetchOneInto(Long.class);
            clubIds.add(clubId);

            // Tighter radius! ~10km bounding box around Tbilisi so they show up easily on the map
            double randomLat = baseLat + (random.nextDouble() * 0.15 - 0.075);
            double randomLng = baseLng + (random.nextDouble() * 0.15 - 0.075);

            Long locationId = dsl.insertInto(LOCATIONS)
                    .set(LOCATIONS.ENTITY_TYPE, "CLUB")
                    .set(LOCATIONS.ENTITY_ID, clubId)
                    .set(LOCATIONS.LATITUDE, BigDecimal.valueOf(randomLat))
                    .set(LOCATIONS.LONGITUDE, BigDecimal.valueOf(randomLng))
                    .set(LOCATIONS.ADDRESS_TEXT, faker.address().streetAddress())
                    .set(LOCATIONS.CREATED_AT, LocalDateTime.now())
                    .returningResult(LOCATIONS.ID)
                    .fetchOneInto(Long.class);

            // Create a Tryout for 80% of the clubs
            if (random.nextInt(100) < 80) {
                dsl.insertInto(TRYOUTS)
                        .set(TRYOUTS.CLUB_ID, clubId)
                        .set(TRYOUTS.TITLE, "Open Trials - " + positions[random.nextInt(positions.length)])
                        .set(TRYOUTS.DESCRIPTION, faker.lorem().paragraph())
                        .set(TRYOUTS.POSITION, positions[random.nextInt(positions.length)])
                        .set(TRYOUTS.AGE_GROUP, "U-21")
                        .set(TRYOUTS.LOCATION_ID, locationId)
                        .set(TRYOUTS.TRYOUT_DATE, LocalDateTime.now().plusDays(random.nextInt(21) + 1)) // Future dates!
                        .set(TRYOUTS.CREATED_BY, userIds.get(random.nextInt(userIds.size())))
                        .execute();
            }
        }

        // --- 3. GENERATE 100 POSTS ---
        for (int i = 0; i < 100; i++) {
            Long authorId = userIds.get(random.nextInt(userIds.size()));
            Long clubId = random.nextInt(100) < 30 ? clubIds.get(random.nextInt(clubIds.size())) : null;

            dsl.insertInto(POSTS)
                    .set(POSTS.AUTHOR_ID, authorId)
                    .set(POSTS.CLUB_ID, clubId)
                    .set(POSTS.CONTENT, faker.lorem().paragraph(random.nextInt(3) + 1))
                    .set(POSTS.IS_PUBLIC, true)
                    .set(POSTS.CREATED_AT, LocalDateTime.now().minusHours(random.nextInt(500)))
                    .execute();
        }

        dsl.insertInto(USERS)
                .set(USERS.EMAIL, "react_dev@demo.com")
                .set(USERS.USERNAME, "react_dev")
                .set(USERS.PASSWORD_HASH, "dev_bypass")
                .set(USERS.CREATED_AT, LocalDateTime.now())
                .onDuplicateKeyIgnore()
                .execute();

        System.out.println("✅ Massive Data Seeding Complete!");
    }
}