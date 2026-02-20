package ge.dola.talanti.security;

import ge.dola.talanti.jooq.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static ge.dola.talanti.jooq.Tables.OAUTH2_LOGINS;
import static ge.dola.talanti.jooq.Tables.USERS;

@Service
public class AuthService {

    private final DSLContext dsl;

    public AuthService(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional
    public UsersRecord processOAuthPostLogin(String email, String provider, String providerId) {
        // 1. Check if this exact OAuth account has logged in before
        var existingLogin = dsl.selectFrom(OAUTH2_LOGINS)
                .where(OAUTH2_LOGINS.PROVIDER.eq(provider))
                .and(OAUTH2_LOGINS.PROVIDER_ID.eq(providerId))
                .fetchOptional();

        if (existingLogin.isPresent()) {
            // Return the existing user linked to this OAuth account
            return dsl.selectFrom(USERS)
                    .where(USERS.ID.eq(existingLogin.get().getUserId()))
                    .fetchOne();
        }

        // 2. If no OAuth login exists, check if we already have a user with this email
        UsersRecord user = dsl.selectFrom(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOptional()
                .orElse(null);

        // 3. If it's a completely new user, insert them into the users table
        if (user == null) {
            String generatedUsername = email.split("@")[0] + "_" + providerId.substring(0, 5);

            user = dsl.insertInto(USERS)
                    .set(USERS.EMAIL, email)
                    .set(USERS.USERNAME, generatedUsername)
                    .set(USERS.PASSWORD_HASH, "") // Empty because they use OAuth
                    .set(USERS.SYSTEM_ROLE, (short) 0) // Default role
                    .set(USERS.CREATED_AT, LocalDateTime.now())
                    .returning()
                    .fetchOne();
        }

        // 4. Link the OAuth provider to the user ID
        dsl.insertInto(OAUTH2_LOGINS)
                .set(OAUTH2_LOGINS.USER_ID, user.getId())
                .set(OAUTH2_LOGINS.PROVIDER, provider)
                .set(OAUTH2_LOGINS.PROVIDER_ID, providerId)
                .set(OAUTH2_LOGINS.CREATED_AT, LocalDateTime.now())
                .execute();

        return user;
    }
}