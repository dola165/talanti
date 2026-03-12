package ge.dola.talanti.config;

import net.datafaker.Faker;
import org.jooq.DSLContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder; // Added
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
    private final PasswordEncoder passwordEncoder; // Added

    public DataSeeder(DSLContext dsl, PasswordEncoder passwordEncoder) { // Updated
        this.dsl = dsl;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int userCount = dsl.fetchCount(dsl.selectFrom(USERS));
        if (userCount > 5) {
            System.out.println("🌱 Database already populated. Skipping massive seeder.");
            return;
        }

        System.out.println("🌱 Starting Talanti Blueprint Seeder...");
        Faker faker = new Faker();
        Random random = new Random();

        // Cache the encoded password to avoid re-hashing Argon2 50+ times (very CPU intensive)
        String commonPasswordHash = passwordEncoder.encode("password");

        // --- 1. CREATE THE 4 TEST PERSONAS ---
        // Pass the common hash to speed up seeding
        Long playerId = createPersona("player@talanti.ge", "test_player", "Beqa Dolidze", (short) 1, "PLAYER", commonPasswordHash);
        Long coachId = createPersona("coach@talanti.ge", "test_coach", "Luka Maisuradze", (short) 3, "CLUB_ADMIN", commonPasswordHash);
        Long agentId = createPersona("agent@talanti.ge", "test_agent", "Elite Scout Agency", (short) 2, "AGENT", commonPasswordHash);
        Long fanId = createPersona("fan@talanti.ge", "test_fan", "Saba Gogichaishvili", (short) 0, "FAN", commonPasswordHash);

        // ... [Rest of your career history and location logic remains the same] ...

        // --- 3. GENERATE BACKGROUND USERS (Noise) ---
        for (int i = 0; i < 50; i++) {
            Long uId = dsl.insertInto(USERS)
                    .set(USERS.EMAIL, faker.internet().emailAddress())
                    .set(USERS.USERNAME, faker.internet().username())
                    .set(USERS.PASSWORD_HASH, commonPasswordHash) // Fixed: Use Argon2 Hash
                    .set(USERS.SYSTEM_ROLE, (short) 1)
                    .set(USERS.CREATED_AT, LocalDateTime.now().minusDays(random.nextInt(365)))
                    .returningResult(USERS.ID).fetchOneInto(Long.class);

            // ... [Rest of user profile generation] ...
        }

        // ... [Rest of your Club, Squad, Post generation logic remains the same] ...

        System.out.println("✅ Seeding Complete! Use 'password' for all test accounts.");
    }

    private Long createPersona(String email, String username, String fullName, short role, String roleName, String passwordHash) {
        Long userId = dsl.select(USERS.ID).from(USERS).where(USERS.EMAIL.eq(email)).fetchOneInto(Long.class);

        if (userId == null) {
            userId = dsl.insertInto(USERS)
                    .set(USERS.EMAIL, email)
                    .set(USERS.USERNAME, username)
                    .set(USERS.PASSWORD_HASH, passwordHash) // Fixed: Use Argon2 Hash
                    .set(USERS.SYSTEM_ROLE, role)
                    .set(USERS.CREATED_AT, LocalDateTime.now()) // Added: Prevent NotNull violation
                    .returningResult(USERS.ID)
                    .fetchOneInto(Long.class);
        }

        // ... [Profile logic remains same but ensure .onDuplicateKeyUpdate() is used if needed] ...
        return userId;
    }
}