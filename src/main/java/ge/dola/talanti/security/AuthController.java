package ge.dola.talanti.security;

import ge.dola.talanti.config.security.JwtProperties;
import ge.dola.talanti.user.dto.CreateUserDto;
import ge.dola.talanti.security.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ge.dola.talanti.jooq.Tables.USERS;
import static ge.dola.talanti.jooq.Tables.USER_PROFILES;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final DSLContext dsl;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties props;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody CreateUserDto dto, HttpServletResponse response) {

        boolean exists = dsl.fetchExists(dsl.selectOne().from(USERS).where(USERS.EMAIL.eq(dto.email())));
        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already in use."));
        }

        // DATABASE FIX: Added CREATED_AT to prevent NotNull constraint violations in Postgres
        Long newUserId = dsl.insertInto(USERS)
                .set(USERS.USERNAME, dto.email().split("@")[0])
                .set(USERS.EMAIL, dto.email())
                .set(USERS.PASSWORD_HASH, passwordEncoder.encode(dto.password()))
                .set(USERS.SYSTEM_ROLE, (short) 0)
                .set(USERS.CREATED_AT, LocalDateTime.now())
                .returningResult(USERS.ID)
                .fetchOneInto(Long.class);

        dsl.insertInto(USER_PROFILES)
                .set(USER_PROFILES.USER_ID, newUserId)
                .set(USER_PROFILES.FULL_NAME, "New User")
                .execute();

        var tokens = jwtService.generateTokens(newUserId, dto.email(), List.of("ROLE_USER"));
        addRefreshCookie(response, tokens.refreshToken(), tokens.refreshExpiresAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "accessToken", tokens.accessToken(),
                "userId", newUserId,
                "role", "USER"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {

        CustomUserDetails user;
        try {
            user = (CustomUserDetails) userDetailsService.loadUserByUsername(request.email());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials."));
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials."));
        }

        var tokens = jwtService.generateTokens(
                user.getUserId(),
                user.getUsername(),
                List.of("ROLE_" + user.getRole())
        );

        addRefreshCookie(response, tokens.refreshToken(), tokens.refreshExpiresAt());

        return ResponseEntity.ok(Map.of(
                "accessToken", tokens.accessToken(),
                "userId", user.getUserId(),
                "role", user.getRole(),
                "fullName", user.getFullName()
        ));
    }

    // --- NEW: THE SILENT REFRESH ENDPOINT ---
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "tl_refresh", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No refresh token provided"));
        }

        try {
            // 1. Parse and validate the token signature
            var jws = jwtService.parse(refreshToken);
            String jti = jws.getPayload().getId();
            Long userId = Long.valueOf(jws.getPayload().getSubject());
            String typ = jws.getPayload().get("typ", String.class);

            // 2. Ensure it's a refresh token AND it's valid in the database
            if (!"refresh".equals(typ) || !jwtService.isRefreshTokenValidAndNotRevoked(jti)) {
                throw new RuntimeException("Invalid or revoked refresh token");
            }

            // 3. Security Best Practice: Token Rotation (Revoke old, issue new)
            jwtService.revokeRefreshToken(jti);

            // 4. Load the user to get fresh roles
            CustomUserDetails userDetails = userDetailsService.loadUserById(userId);
            List<String> authorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // 5. Generate a brand new pair of tokens
            var newTokens = jwtService.generateTokens(
                    userId,
                    userDetails.getUsername(),
                    authorities
            );

            // 6. Set the NEW Refresh Token in the HttpOnly cookie using your existing helper
            addRefreshCookie(response, newTokens.refreshToken(), newTokens.refreshExpiresAt());

            // 7. Return the new Access Token to React
            return ResponseEntity.ok(Map.of(
                    "accessToken", newTokens.accessToken(),
                    "expiresAt", newTokens.accessExpiresAt().toEpochMilli()
            ));

        } catch (Exception e) {
            // If anything fails (expired or tampered), wipe the cookie so the frontend knows to log them out
            var cookieBuilder = ResponseCookie.from(props.getCookie().getRefreshName(), "")
                    .httpOnly(true)
                    .secure(Boolean.TRUE.equals(props.getCookie().getSecure()))
                    .path("/api/auth")
                    .maxAge(0);

            if (props.getCookie().getDomain() != null && !props.getCookie().getDomain().isEmpty()) {
                cookieBuilder.domain(props.getCookie().getDomain());
            }

            response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Refresh token expired or invalid"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "tl_refresh", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken != null) {
            try {
                String jti = jwtService.parse(refreshToken).getPayload().getId();
                jwtService.revokeRefreshToken(jti);
            } catch (Exception ignored) {}
        }

        // NULL SAFETY FIX: Use Boolean.TRUE.equals() to prevent NullPointerException
        var cookieBuilder = ResponseCookie.from(props.getCookie().getRefreshName(), "")
                .httpOnly(true)
                .secure(Boolean.TRUE.equals(props.getCookie().getSecure()))
                .path("/api/auth")
                .maxAge(0);

        // MATCH DOMAIN: If a domain was set during creation, it must be set here to successfully delete it
        if (props.getCookie().getDomain() != null && !props.getCookie().getDomain().isEmpty()) {
            cookieBuilder.domain(props.getCookie().getDomain());
        }

        response.setHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private void addRefreshCookie(HttpServletResponse res, String token, Instant exp) {
        long seconds = Math.max(0, exp.getEpochSecond() - Instant.now().getEpochSecond());

        // NULL SAFETY FIX: Use Boolean.TRUE.equals()
        var cookieBuilder = ResponseCookie.from(props.getCookie().getRefreshName(), token)
                .httpOnly(true)
                .secure(Boolean.TRUE.equals(props.getCookie().getSecure()))
                .sameSite(props.getCookie().getSameSite() != null ? props.getCookie().getSameSite() : "Strict")
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(seconds));

        // EMPTY STRING SAFETY: Don't set domain if it's empty, otherwise browser rejects it
        if (props.getCookie().getDomain() != null && !props.getCookie().getDomain().isEmpty()) {
            cookieBuilder.domain(props.getCookie().getDomain());
        }

        res.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }
}