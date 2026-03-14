package ge.dola.talanti.config;

import net.datafaker.Faker;
import org.jooq.DSLContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(DSLContext dsl, PasswordEncoder passwordEncoder) {
        this.dsl = dsl;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Prevent duplicate spam on restart
        if (dsl.fetchCount(dsl.selectFrom(CLUBS)) > 0) {
            System.out.println("🌱 Database already populated. Skipping massive seeder.");
            return;
        }

        System.out.println("🌱 Starting Talanti Blueprint Seeder...");
        Faker faker = new Faker();
        Random random = new Random();

        // Cache the encoded password to avoid re-hashing Argon2 50+ times (very CPU intensive)
        String commonPasswordHash = passwordEncoder.encode("password");

        // --- 1. CREATE THE 4 TEST PERSONAS ---
        Long playerId = createPersona("player@talanti.ge", "test_player", "Beqa Dolidze", (short) 1, "PLAYER", commonPasswordHash);
        Long coachId = createPersona("coach@talanti.ge", "test_coach", "Luka Maisuradze", (short) 3, "CLUB_ADMIN", commonPasswordHash);
        Long agentId = createPersona("agent@talanti.ge", "test_agent", "Elite Scout Agency", (short) 2, "AGENT", commonPasswordHash);
        Long fanId = createPersona("fan@talanti.ge", "test_fan", "Saba Gogichaishvili", (short) 0, "FAN", commonPasswordHash);

        List<Long> allUserIds = new ArrayList<>(List.of(playerId, coachId, agentId, fanId));

        // --- 1.5 ADD CAREER HISTORY TO THE TEST PLAYER ---
        dsl.insertInto(CAREER_HISTORY)
                .set(CAREER_HISTORY.USER_ID, playerId)
                .set(CAREER_HISTORY.CLUB_NAME, "Experimentuli")
                .set(CAREER_HISTORY.SEASON, "2024/25")
                .set(CAREER_HISTORY.CATEGORY, "First Team")
                .set(CAREER_HISTORY.APPEARANCES, 14)
                .set(CAREER_HISTORY.GOALS, 2)
                .set(CAREER_HISTORY.ASSISTS, 5)
                .set(CAREER_HISTORY.CLEAN_SHEETS, 0)
                .execute();

        // --- 2. CREATE A SPECIFIC CLUB FOR THE COACH ---
        Long locationId = dsl.insertInto(LOCATIONS)
                .set(LOCATIONS.LATITUDE, BigDecimal.valueOf(41.7151))
                .set(LOCATIONS.LONGITUDE, BigDecimal.valueOf(44.8271))
                .set(LOCATIONS.ADDRESS_TEXT, "Dinamo Arena, Tbilisi")
                .set(LOCATIONS.ENTITY_TYPE, "CLUB")
                .set(LOCATIONS.ENTITY_ID, 0L)
                .returningResult(LOCATIONS.ID).fetchOneInto(Long.class);

        Long dinamoId = dsl.insertInto(CLUBS)
                .set(CLUBS.NAME, "FC Dinamo Tbilisi")
                .set(CLUBS.DESCRIPTION, "Official professional club and academy.")
                .set(CLUBS.TYPE, "Professional")
                .set(CLUBS.IS_OFFICIAL, true)
                .set(CLUBS.LOCATION_ID, locationId)
                .set(CLUBS.CREATED_BY, coachId)
                .returningResult(CLUBS.ID).fetchOneInto(Long.class);

        dsl.update(LOCATIONS).set(LOCATIONS.ENTITY_ID, dinamoId).where(LOCATIONS.ID.eq(locationId)).execute();

        // 🚀 CRITICAL FIX: LINK THE COACH TO THE CLUB AS ADMIN 🚀
        dsl.insertInto(CLUB_MEMBERSHIPS)
                .set(CLUB_MEMBERSHIPS.CLUB_ID, dinamoId)
                .set(CLUB_MEMBERSHIPS.USER_ID, coachId)
                .set(CLUB_MEMBERSHIPS.ROLE, "ADMIN")
                .execute();

        // Give Dinamo a Squad
        Long dinamoSquadId = dsl.insertInto(SQUADS)
                .set(SQUADS.CLUB_ID, dinamoId)
                .set(SQUADS.NAME, "Dinamo U16 Boys")
                .set(SQUADS.CATEGORY, "U16")
                .set(SQUADS.GENDER, "MALE")
                .set(SQUADS.HEAD_COACH_ID, coachId)
                .returningResult(SQUADS.ID).fetchOneInto(Long.class);

        List<Long> clubIds = new ArrayList<>(List.of(dinamoId));

        // --- 3. GENERATE BACKGROUND USERS (Noise) ---
        for (int i = 0; i < 50; i++) {
            Long uId = dsl.insertInto(USERS)
                    .set(USERS.EMAIL, faker.internet().emailAddress())
                    .set(USERS.USERNAME, faker.internet().username())
                    .set(USERS.PASSWORD_HASH, commonPasswordHash) // Fixed Argon2 Hash
                    .set(USERS.SYSTEM_ROLE, (short) 1)
                    .set(USERS.CREATED_AT, LocalDateTime.now().minusDays(random.nextInt(365)))
                    .returningResult(USERS.ID).fetchOneInto(Long.class);
            allUserIds.add(uId);

            dsl.insertInto(USER_PROFILES)
                    .set(USER_PROFILES.USER_ID, uId)
                    .set(USER_PROFILES.FULL_NAME, faker.name().fullName())
                    .set(USER_PROFILES.BIO, faker.lorem().sentence(5))
                    .set(USER_PROFILES.POSITION, faker.options().option("Striker", "Midfielder", "Defender", "Goalkeeper"))
                    .set(USER_PROFILES.PREFERRED_FOOT, random.nextBoolean() ? "Right" : "Left")
                    .set(USER_PROFILES.AVAILABILITY_STATUS, faker.options().option("FREE_AGENT", "IN_CLUB", "OPEN_TO_OFFERS"))
                    .set(USER_PROFILES.HEIGHT_CM, 160 + random.nextInt(40))
                    .set(USER_PROFILES.WEIGHT_KG, 60 + random.nextInt(30))
                    .execute();
        }

        // --- 4. GENERATE BACKGROUND CLUBS & MAP DATA ---
        String[] clubTypes = {"Professional", "Amateur", "Academy", "University"};
        for (int i = 0; i < 20; i++) {
            double lat = 41.7151 + (random.nextDouble() * 0.2 - 0.1);
            double lng = 44.8271 + (random.nextDouble() * 0.2 - 0.1);

            Long locId = dsl.insertInto(LOCATIONS)
                    .set(LOCATIONS.LATITUDE, BigDecimal.valueOf(lat))
                    .set(LOCATIONS.LONGITUDE, BigDecimal.valueOf(lng))
                    .set(LOCATIONS.ADDRESS_TEXT, faker.address().streetAddress() + ", Tbilisi")
                    .set(LOCATIONS.ENTITY_TYPE, "CLUB")
                    .set(LOCATIONS.ENTITY_ID, 0L)
                    .returningResult(LOCATIONS.ID).fetchOneInto(Long.class);

            Long cId = dsl.insertInto(CLUBS)
                    .set(CLUBS.NAME, faker.team().name() + " FC")
                    .set(CLUBS.DESCRIPTION, faker.lorem().paragraph(2))
                    .set(CLUBS.TYPE, clubTypes[random.nextInt(clubTypes.length)])
                    .set(CLUBS.IS_OFFICIAL, random.nextBoolean())
                    .set(CLUBS.LOCATION_ID, locId)
                    .set(CLUBS.CREATED_AT, LocalDateTime.now().minusDays(random.nextInt(365)))
                    .set(CLUBS.CREATED_BY, allUserIds.get(random.nextInt(allUserIds.size())))
                    .returningResult(CLUBS.ID).fetchOneInto(Long.class);

            clubIds.add(cId);
            dsl.update(LOCATIONS).set(LOCATIONS.ENTITY_ID, cId).where(LOCATIONS.ID.eq(locId)).execute();

            Long sqId = dsl.insertInto(SQUADS)
                    .set(SQUADS.CLUB_ID, cId)
                    .set(SQUADS.NAME, "First Team")
                    .set(SQUADS.CATEGORY, "SENIOR")
                    .returningResult(SQUADS.ID).fetchOneInto(Long.class);

            // 50% chance to create a Match Request for Map
            if (random.nextBoolean()) {
                dsl.insertInto(MATCH_REQUESTS)
                        .set(MATCH_REQUESTS.CLUB_ID, cId)
                        .set(MATCH_REQUESTS.SQUAD_ID, sqId)
                        .set(MATCH_REQUESTS.CREATOR_ID, allUserIds.get(random.nextInt(allUserIds.size())))
                        .set(MATCH_REQUESTS.DESIRED_DATE, LocalDateTime.now().plusDays(random.nextInt(14) + 1))
                        .set(MATCH_REQUESTS.LOCATION_PREF, "CAN_HOST")
                        .set(MATCH_REQUESTS.LOCATION_ID, locId)
                        .set(MATCH_REQUESTS.STATUS, "OPEN")
                        .execute();
            }

            // 50% chance to create a Tryout for Map
            if (random.nextBoolean()) {
                dsl.insertInto(TRYOUTS)
                        .set(TRYOUTS.CLUB_ID, cId)
                        .set(TRYOUTS.TITLE, "Open Trial: " + faker.options().option("Spring", "Summer", "Winter"))
                        .set(TRYOUTS.DESCRIPTION, faker.lorem().sentence(5))
                        .set(TRYOUTS.POSITION, "Any")
                        .set(TRYOUTS.AGE_GROUP, "U-18")
                        .set(TRYOUTS.LOCATION_ID, locId)
                        .set(TRYOUTS.TRYOUT_DATE, LocalDateTime.now().plusDays(random.nextInt(21) + 1))
                        .set(TRYOUTS.CREATED_BY, coachId)
                        .execute();
            }
        }

        // --- 5. GENERATE POSTS FOR FEED ---
        for (int i = 0; i < 50; i++) {
            Long authorId = allUserIds.get(random.nextInt(allUserIds.size()));
            Long clubId = random.nextInt(100) < 40 ? clubIds.get(random.nextInt(clubIds.size())) : null;

            dsl.insertInto(POSTS)
                    .set(POSTS.AUTHOR_ID, authorId)
                    .set(POSTS.CLUB_ID, clubId)
                    .set(POSTS.CONTENT, faker.lorem().paragraph(random.nextInt(3) + 1))
                    .set(POSTS.IS_PUBLIC, true)
                    .set(POSTS.CREATED_AT, LocalDateTime.now().minusHours(random.nextInt(500)))
                    .execute();
        }

        // --- 6. INJECT FAKE APPLICATIONS TO THE COACH'S TRYOUT ---
        Long coachTryoutId = dsl.select(TRYOUTS.ID).from(TRYOUTS)
                .where(TRYOUTS.CREATED_BY.eq(coachId)).limit(1).fetchOneInto(Long.class);

        if (coachTryoutId != null) {
            for (int i = 1; i <= 4; i++) {
                dsl.insertInto(TRYOUT_APPLICATIONS)
                        .set(TRYOUT_APPLICATIONS.TRYOUT_ID, coachTryoutId)
                        .set(TRYOUT_APPLICATIONS.USER_ID, allUserIds.get(i + 5))
                        .set(TRYOUT_APPLICATIONS.STATUS, "PENDING")
                        .set(TRYOUT_APPLICATIONS.APPLIED_AT, LocalDateTime.now().minusHours(random.nextInt(48)))
                        .execute();
            }
        }

        System.out.println("✅ Seeding Complete! Test accounts, Clubs, Squads, Matches, Tryouts, and Posts are ready.");
    }

    private Long createPersona(String email, String username, String fullName, short role, String roleName, String passwordHash) {
        Long userId = dsl.select(USERS.ID).from(USERS).where(USERS.EMAIL.eq(email)).fetchOneInto(Long.class);

        if (userId == null) {
            userId = dsl.insertInto(USERS)
                    .set(USERS.EMAIL, email)
                    .set(USERS.USERNAME, username)
                    .set(USERS.PASSWORD_HASH, passwordHash)
                    .set(USERS.SYSTEM_ROLE, role)
                    .set(USERS.CREATED_AT, LocalDateTime.now())
                    .returningResult(USERS.ID)
                    .fetchOneInto(Long.class);
        }

        dsl.insertInto(USER_PROFILES)
                .set(USER_PROFILES.USER_ID, userId)
                .set(USER_PROFILES.FULL_NAME, fullName)
                .set(USER_PROFILES.BIO, "I am a test " + roleName + " account.")
                .set(USER_PROFILES.POSITION, role == 1 ? "Midfielder" : null)
                .set(USER_PROFILES.PREFERRED_FOOT, role == 1 ? "Right" : null)
                .set(USER_PROFILES.AVAILABILITY_STATUS, role == 1 ? "FREE_AGENT" : null)
                .set(USER_PROFILES.HEIGHT_CM, role == 1 ? 185 : null)
                .set(USER_PROFILES.WEIGHT_KG, role == 1 ? 78 : null)
                .onDuplicateKeyIgnore()
                .execute();

        return userId;
    }
}