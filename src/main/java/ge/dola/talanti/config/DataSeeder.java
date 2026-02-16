package ge.dola.talanti.config;

import ge.dola.talanti.jooq.tables.records.UsersRecord;
import ge.dola.talanti.jooq.tables.records.UserProfilesRecord;
import org.jooq.DSLContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static ge.dola.talanti.jooq.Tables.USERS;
import static ge.dola.talanti.jooq.Tables.USER_PROFILES;

@Component
public class DataSeeder implements CommandLineRunner {

    private final DSLContext dsl;

    public DataSeeder(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Only insert if the table is empty
        if (dsl.fetchCount(USERS) == 0) {
            System.out.println("🌱 Seeding Test User...");

            // 1. Create a Fake User (ID will be 1)
            UsersRecord user = dsl.newRecord(USERS);
            user.setUsername("dola_dev");
            user.setEmail("dev@talanti.ge");
            user.setPasswordHash("dummy_hash"); // No security needed yet
            user.store();

            Long newUserId = user.getId();

            // 2. Create the Profile for this user
            UserProfilesRecord profile = dsl.newRecord(USER_PROFILES);
            profile.setUserId(newUserId);
            profile.setFullName("Dola Developer");
            profile.setPosition("Midfielder");
            profile.setPreferredFoot("Right");
            profile.store();

            System.out.println("✅ User 1 Created! You can now chat as 'dola_dev'.");
        }
    }
}