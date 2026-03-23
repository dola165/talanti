package ge.dola.talanti.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import ge.dola.talanti.config.security.JwtProperties;
import ge.dola.talanti.jooq.tables.records.Oauth2LoginsRecord;
import ge.dola.talanti.jooq.tables.records.UsersRecord;
import ge.dola.talanti.security.dto.LoginRequest;
import ge.dola.talanti.user.dto.CreateUserDto;
import ge.dola.talanti.user.UserType; // STRICT IMPORT
import ge.dola.talanti.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static ge.dola.talanti.jooq.Tables.*;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Set<UserType> SELF_SELECTABLE_SIGNUP_ROLES = Set.of(UserType.PLAYER, UserType.FAN);

    private final DSLContext dsl;
    private final UserRepository userRepository;
    private final OAuth2LoginRepository oauth2LoginRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final JwtProperties props;
    private final GoogleTokenValidator googleTokenValidator;

    @Transactional
    public void registerUser(CreateUserDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("Email is already in use.");
        }

        String baseUsername = dto.email().split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 5);
        UserType initialRole = resolveInitialRole(dto.role());

        Long userId = dsl.insertInto(USERS)
                .set(USERS.USERNAME, baseUsername)
                .set(USERS.EMAIL, dto.email())
                .set(USERS.PASSWORD_HASH, passwordEncoder.encode(dto.password()))
                .set(USERS.USER_TYPE, initialRole.name())
                .set(USERS.CREATED_AT, LocalDateTime.now())
                .returningResult(USERS.ID)
                .fetchOneInto(Long.class);

        dsl.insertInto(USER_PROFILES)
                .set(USER_PROFILES.USER_ID, userId)
                .execute();
    }

    @Transactional
    public String processLogin(LoginRequest request, HttpServletResponse response) {
        UsersRecord user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        // Fail securely if password_hash is null (e.g., they only registered via OAuth)
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        var tokens = jwtService.generateTokens(
                user.getId(),
                user.getUsername(),
                List.of("ROLE_" + user.getUserType().toUpperCase())
        );

        setRefreshCookie(response, tokens.refreshToken(), tokens.refreshExpiresAt());
        return tokens.accessToken();
    }

    @Transactional
    public String processGoogleLogin(String googleTokenString, HttpServletResponse response) {
        GoogleIdToken.Payload payload = googleTokenValidator.verify(googleTokenString);

        String email = payload.getEmail();
        String googleId = payload.getSubject();
        boolean isEmailVerified = Boolean.TRUE.equals(payload.getEmailVerified());

        UsersRecord user = processOAuthPostLogin(email, "google", googleId, isEmailVerified);

        var tokens = jwtService.generateTokens(
                user.getId(),
                user.getEmail(),
                List.of("ROLE_" + user.getUserType().toUpperCase())
        );

        setRefreshCookie(response, tokens.refreshToken(), tokens.refreshExpiresAt());
        return tokens.accessToken();
    }

    public void processLogout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        if (refreshToken != null) {
            try {
                Jws<Claims> jws = jwtService.parse(refreshToken, JwtService.TOKEN_TYPE_REFRESH);
                jwtService.revokeRefreshToken(jws.getPayload().getId());
            } catch (Exception ignored) {
                // Ignore expired or malformed tokens on logout
            }
        }
        setRefreshCookie(response, "", Instant.now());
    }

    @Transactional
    public String processRefresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        if (refreshToken == null) {
            throw new IllegalArgumentException("No refresh token provided.");
        }

        Jws<Claims> jws;
        try {
            jws = jwtService.parse(refreshToken, JwtService.TOKEN_TYPE_REFRESH);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or expired refresh token.");
        }

        String jti = jws.getPayload().getId();
        if (!jwtService.consumeRefreshToken(jti)) {
            throw new IllegalArgumentException("Revoked or invalid token type.");
        }

        Long userId = Long.valueOf(jws.getPayload().getSubject());
        CustomUserDetails user = userDetailsService.loadUserById(userId);

        var tokens = jwtService.generateTokens(
                user.getUserId(),
                user.getUsername(),
                List.of("ROLE_" + user.getRole())
        );
        setRefreshCookie(response, tokens.refreshToken(), tokens.refreshExpiresAt());

        return tokens.accessToken();
    }

    private UsersRecord processOAuthPostLogin(String email, String provider, String providerId, boolean isEmailVerified) {
        Optional<Oauth2LoginsRecord> oauthLogin = oauth2LoginRepository.findByProviderAndId(provider, providerId);
        if (oauthLogin.isPresent()) {
            return userRepository.findById(oauthLogin.get().getUserId())
                    .orElseThrow(() -> new IllegalStateException("OAuth linked user no longer exists."));
        }

        Long userId;
        Optional<UsersRecord> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            if (!isEmailVerified) {
                throw new BadCredentialsException("Unverified email from provider. Cannot securely link accounts.");
            }
            userId = existingUser.get().getId();

            // ANTI-HIJACK PATCH: Nullify the password to prevent an attacker from logging in with a previously set password.
            dsl.update(USERS)
                    .setNull(USERS.PASSWORD_HASH)
                    .where(USERS.ID.eq(userId))
                    .execute();
        } else {
            String baseUsername = email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 5);

            userId = dsl.insertInto(USERS)
                    .set(USERS.USERNAME, baseUsername)
                    .set(USERS.EMAIL, email)
                    .setNull(USERS.PASSWORD_HASH) // STRICT ENFORCEMENT: No dummy passwords.
                    .set(USERS.USER_TYPE, UserType.PLAYER.name())
                    .set(USERS.CREATED_AT, LocalDateTime.now())
                    .returningResult(USERS.ID)
                    .fetchOneInto(Long.class);

            dsl.insertInto(USER_PROFILES)
                    .set(USER_PROFILES.USER_ID, userId)
                    .execute();
        }

        dsl.insertInto(OAUTH2_LOGINS)
                .set(OAUTH2_LOGINS.USER_ID, userId)
                .set(OAUTH2_LOGINS.PROVIDER, provider)
                .set(OAUTH2_LOGINS.PROVIDER_ID, providerId)
                .set(OAUTH2_LOGINS.CREATED_AT, LocalDateTime.now())
                .execute();

        return userRepository.findById(userId).orElseThrow();
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (props.getCookie().getRefreshName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private UserType resolveInitialRole(UserType requestedRole) {
        if (requestedRole == null) {
            return UserType.PLAYER;
        }
        if (!SELF_SELECTABLE_SIGNUP_ROLES.contains(requestedRole)) {
            throw new IllegalArgumentException("Only PLAYER or FAN can be selected during signup.");
        }
        return requestedRole;
    }

    private void setRefreshCookie(HttpServletResponse response, String token, Instant expiresAt) {
        long seconds = Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        var cookieBuilder = ResponseCookie.from(props.getCookie().getRefreshName(), token)
                .httpOnly(true)
                .secure(Boolean.TRUE.equals(props.getCookie().getSecure()))
                .sameSite(props.getCookie().getSameSite() != null ? props.getCookie().getSameSite() : "Strict")
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(seconds));

        if (props.getCookie().getDomain() != null && !props.getCookie().getDomain().isEmpty()) {
            cookieBuilder.domain(props.getCookie().getDomain());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }
}
