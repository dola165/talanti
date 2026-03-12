package ge.dola.talanti.security;

import ge.dola.talanti.jooq.tables.records.Oauth2LoginsRecord;
import ge.dola.talanti.jooq.tables.records.UsersRecord;
import ge.dola.talanti.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static ge.dola.talanti.jooq.Tables.OAUTH2_LOGINS;
import static ge.dola.talanti.jooq.Tables.USERS;
import static ge.dola.talanti.jooq.Tables.USER_PROFILES;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final DSLContext dsl;
    private final UserRepository userRepository;
    private final OAuth2LoginRepository oauth2LoginRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsersRecord processOAuthPostLogin(String email, String provider, String providerId) {
        // 1. Check if OAuth2 login already exists
        Optional<Oauth2LoginsRecord> oauthLogin = oauth2LoginRepository.findByProviderAndId(provider, providerId);
        if (oauthLogin.isPresent()) {
            return userRepository.findById(oauthLogin.get().getUserId()).orElseThrow();
        }

        // 2. Check if user with this email already exists
        Optional<UsersRecord> existingUser = userRepository.findByEmail(email);
        Long userId;

        if (existingUser.isPresent()) {
            userId = existingUser.get().getId();
        } else {
            // CRITICAL FIX: Generate a random secure password to satisfy the NOT NULL DB constraint
            String dummyPassword = UUID.randomUUID().toString();
            String baseUsername = email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 5);

            userId = dsl.insertInto(USERS)
                    .set(USERS.USERNAME, baseUsername)
                    .set(USERS.EMAIL, email)
                    .set(USERS.PASSWORD_HASH, passwordEncoder.encode(dummyPassword)) // <-- Fixes the crash
                    .set(USERS.SYSTEM_ROLE, (short) 0)
                    .set(USERS.CREATED_AT, LocalDateTime.now())
                    .returningResult(USERS.ID)
                    .fetchOneInto(Long.class);

            // CRITICAL FIX: Create the Profile record so the frontend Onboarding interceptor works
            dsl.insertInto(USER_PROFILES)
                    .set(USER_PROFILES.USER_ID, userId)
                    .set(USER_PROFILES.FULL_NAME, "New User")
                    .execute();
        }

        // 3. Link the provider to the account
        dsl.insertInto(OAUTH2_LOGINS)
                .set(OAUTH2_LOGINS.USER_ID, userId)
                .set(OAUTH2_LOGINS.PROVIDER, provider)
                .set(OAUTH2_LOGINS.PROVIDER_ID, providerId)
                .set(OAUTH2_LOGINS.CREATED_AT, LocalDateTime.now())
                .execute();

        return userRepository.findById(userId).orElseThrow();
    }
}