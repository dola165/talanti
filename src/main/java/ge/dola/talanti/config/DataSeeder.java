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
        // Temporarily commented out to force regeneration if needed
        // int userCount = dsl.fetchCount(dsl.selectFrom(USERS));
        // if (userCount > 5) {
        //     System.out.println("🌱 Database already populated. Skipping massive seeder.");
        //     return;
        // }

        System.out.println("🌱 Starting Massive Data Seeder with Datafaker...");
        Faker faker = new Faker();
        Random random = new Random();

        List<Long> userIds = new ArrayList<>();
        List<Long> clubIds = new ArrayList<>();
        List<Long> postIds = new ArrayList<>();

        // --- 1. GENERATE 100 USERS ---
        for (int i = 0; i < 100; i++) {
            Long userId = dsl.insertInto(USERS)
                    .set(USERS.EMAIL, faker.internet().emailAddress())
                    .set(USERS.USERNAME, faker.internet().username())
                    .set(USERS.PASSWORD_HASH, "hashed_password")
                    .set(USERS.SYSTEM_ROLE, (short) 0) // ADDED SYSTEM_ROLE
                    .set(USERS.CREATED_AT, LocalDateTime.now().minusDays(random.nextInt(365)))
                    .returningResult(USERS.ID)
                    .fetchOneInto(Long.class);

            userIds.add(userId);

            dsl.insertInto(USER_PROFILES)
                    .set(USER_PROFILES.USER_ID, userId)
                    .set(USER_PROFILES.FULL_NAME, faker.name().fullName())
                    .set(USER_PROFILES.BIO, faker.lorem().sentence(5)) // Kept short to prevent DB truncation
                    .set(USER_PROFILES.POSITION, faker.options().option("Striker", "Midfielder", "Defender", "Goalkeeper"))
                    .set(USER_PROFILES.PREFERRED_FOOT, random.nextBoolean() ? "Right" : "Left")
                    .execute();
        }

        // --- 2. GENERATE 30 CLUBS & LOCATIONS ---
        String[] clubTypes = {"Professional", "Amateur", "Academy", "University"};

        for (int i = 0; i < 30; i++) {
            // Drop them near Tbilisi (Lat: 41.7151, Lng: 44.8271)
            double lat = 41.7151 + (random.nextDouble() * 0.2 - 0.1);
            double lng = 44.8271 + (random.nextDouble() * 0.2 - 0.1);

            Long locationId = dsl.insertInto(LOCATIONS)
                    .set(LOCATIONS.LATITUDE, BigDecimal.valueOf(lat))
                    .set(LOCATIONS.LONGITUDE, BigDecimal.valueOf(lng))
                    .set(LOCATIONS.ADDRESS_TEXT, faker.address().streetAddress() + ", Tbilisi")
                    .set(LOCATIONS.ENTITY_TYPE, "CLUB")
                    .set(LOCATIONS.ENTITY_ID, 0L) // temporary
                    .returningResult(LOCATIONS.ID)
                    .fetchOneInto(Long.class);

            Long clubId = dsl.insertInto(CLUBS)
                    .set(CLUBS.NAME, faker.team().name() + " FC")
                    .set(CLUBS.DESCRIPTION, faker.lorem().paragraph(2))
                    .set(CLUBS.TYPE, clubTypes[random.nextInt(clubTypes.length)])
                    .set(CLUBS.IS_OFFICIAL, random.nextBoolean())
                    .set(CLUBS.LOCATION_ID, locationId)
                    .set(CLUBS.CREATED_AT, LocalDateTime.now().minusDays(random.nextInt(365)))
                    .set(CLUBS.CREATED_BY, userIds.get(random.nextInt(userIds.size())))
                    // --- NEW CONTACT AND EXTERNAL LINK FIELDS ---
                    .set(CLUBS.STORE_URL, "https://www.facebook.com/marketplace/category/sporting-goods/")
                    .set(CLUBS.GOFUNDME_URL, "https://www.gofundme.com/f/help-youth-football")
                    .set(CLUBS.PHONE_NUMBER, faker.phoneNumber().cellPhone())
                    .set(CLUBS.EMAIL, faker.internet().safeEmailAddress())
                    .returningResult(CLUBS.ID)
                    .fetchOneInto(Long.class);

            clubIds.add(clubId);

            // Update location with actual club ID
            dsl.update(LOCATIONS)
                    .set(LOCATIONS.ENTITY_ID, clubId)
                    .where(LOCATIONS.ID.eq(locationId))
                    .execute();

            // Create some fake tryouts for half of the clubs
            if (random.nextBoolean()) {
                dsl.insertInto(TRYOUTS)
                        .set(TRYOUTS.CLUB_ID, clubId)
                        .set(TRYOUTS.TITLE, "Open Trial: " + faker.options().option("Spring", "Summer", "Winter"))
                        .set(TRYOUTS.DESCRIPTION, faker.lorem().sentence(5))
                        .set(TRYOUTS.POSITION, faker.options().option("Any", "Goalkeeper", "Striker"))
                        .set(TRYOUTS.AGE_GROUP, "U-21")
                        .set(TRYOUTS.LOCATION_ID, locationId)
                        .set(TRYOUTS.TRYOUT_DATE, LocalDateTime.now().plusDays(random.nextInt(21) + 1)) // Future dates!
                        .set(TRYOUTS.CREATED_BY, userIds.get(random.nextInt(userIds.size())))
                        .execute();
            }
            // --- GENERATE A SQUAD FOR THE CLUB ---
            Long squadId = dsl.insertInto(SQUADS)
                    .set(SQUADS.CLUB_ID, clubId)
                    .set(SQUADS.NAME, faker.team().name() + " U16 Boys")
                    .set(SQUADS.CATEGORY, "U16")
                    .set(SQUADS.GENDER, "MALE")
                    .set(SQUADS.CREATED_AT, LocalDateTime.now())
                    .returningResult(SQUADS.ID)
                    .fetchOneInto(Long.class);

            Long clubCreatorId = userIds.get(random.nextInt(userIds.size()));

            // --- GENERATE A MATCH REQUEST (50% Chance) ---
            if (random.nextBoolean()) {
                dsl.insertInto(MATCH_REQUESTS)
                        .set(MATCH_REQUESTS.CLUB_ID, clubId)
                        .set(MATCH_REQUESTS.SQUAD_ID, squadId)
                        .set(MATCH_REQUESTS.CREATOR_ID, clubCreatorId)
                        .set(MATCH_REQUESTS.DESIRED_DATE, LocalDateTime.now().plusDays(random.nextInt(14) + 1))
                        .set(MATCH_REQUESTS.LOCATION_PREF, "CAN_HOST")
                        .set(MATCH_REQUESTS.LOCATION_ID, locationId)
                        .set(MATCH_REQUESTS.STATUS, "OPEN")
                        .set(MATCH_REQUESTS.CREATED_AT, LocalDateTime.now())
                        .execute();
            }

            // --- GENERATE A TRYOUT (50% Chance) ---
            if (random.nextBoolean()) {
                dsl.insertInto(TRYOUTS)
                        .set(TRYOUTS.CLUB_ID, clubId)
                        .set(TRYOUTS.TITLE, "Open Trial: " + faker.options().option("Spring", "Summer", "Winter"))
                        .set(TRYOUTS.DESCRIPTION, faker.lorem().sentence(5))
                        .set(TRYOUTS.POSITION, faker.options().option("Any", "Goalkeeper", "Striker"))
                        .set(TRYOUTS.AGE_GROUP, "U-21")
                        .set(TRYOUTS.LOCATION_ID, locationId)
                        .set(TRYOUTS.TRYOUT_DATE, LocalDateTime.now().plusDays(random.nextInt(21) + 1)) // Future dates!
                        .set(TRYOUTS.CREATED_BY, clubCreatorId)
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

        // --- 4. GENERATE DEV USER ---
        dsl.insertInto(USERS)
                .set(USERS.EMAIL, "react_dev@demo.com")
                .set(USERS.USERNAME, "react_dev")
                .set(USERS.PASSWORD_HASH, "dev_bypass")
                .set(USERS.SYSTEM_ROLE, (short) 0) // ADDED SYSTEM_ROLE
                .set(USERS.CREATED_AT, LocalDateTime.now())
                .onDuplicateKeyIgnore()
                .execute();

        Long devId = dsl.select(USERS.ID).from(USERS).where(USERS.USERNAME.eq("react_dev")).fetchOneInto(Long.class);

        dsl.insertInto(USER_PROFILES)
                .set(USER_PROFILES.USER_ID, devId)
                .set(USER_PROFILES.FULL_NAME, "React Developer")
                .set(USER_PROFILES.BIO, "I am the developer of this application.")
                .set(USER_PROFILES.POSITION, "Midfielder")
                .onDuplicateKeyIgnore()
                .execute();

        System.out.println("✅ Seeding Complete! " + userIds.size() + " Users, " + clubIds.size() + " Clubs, and 100 Posts created.");
    }
}