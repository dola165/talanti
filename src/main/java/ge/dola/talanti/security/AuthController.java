package ge.dola.talanti.security;

import com.nimbusds.oauth2.sdk.TokenRequest;
import ge.dola.talanti.jooq.tables.records.UsersRecord;
import ge.dola.talanti.security.dto.AuthResponseDto;
import ge.dola.talanti.security.dto.GoogleUserInfoDto;
import ge.dola.talanti.security.dto.TokenRequestDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import static ge.dola.talanti.jooq.Tables.USERS;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenProvider tokenProvider;
    private final GoogleTokenValidator googleTokenValidator;
    private final DSLContext dsl;


    @PostMapping("/google")
    public ResponseEntity<?> authenticateGoogle(@RequestBody TokenRequestDto request, HttpServletResponse response) {
        try {
            // 1. Verify the token with Google's servers
            GoogleUserInfoDto userInfo = googleTokenValidator.validate(request.getToken());

            // 2. Find or Create the user in our database
            UsersRecord user = authService.processOAuthPostLogin(
                    userInfo.getEmail(),
                    "google",
                    userInfo.getSubject() // Google's unique account ID
            );

            // 3. Create the internal session token
            String internalJwt = tokenProvider.createToken(user.getId());

            // 4. Set it as an HttpOnly Cookie
            attachAuthCookie(response, internalJwt);

            // Send non-sensitive data back to React for the UI state
            return ResponseEntity.ok(new AuthResponseDto("Login successful", user.getId(), user.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Authentication failed: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Overwrite the cookie with a blank one that expires immediately
        Cookie cookie = new Cookie(CookieAuthFilter.COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok("Logged out successfully");
    }

    private void attachAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(CookieAuthFilter.COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Ensures it only sends over HTTPS (or localhost)
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 1 day
        response.addCookie(cookie);
    }


    @PostMapping("/dev-login")
    @Transactional
    public ResponseEntity<?> devLogin(@RequestParam String email, HttpServletResponse response) {
        // 1. Find or create the dev user
        UsersRecord user = dsl.selectFrom(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOptional()
                .orElseGet(() -> {
                    return dsl.insertInto(USERS)
                            .set(USERS.EMAIL, email)
                            .set(USERS.USERNAME, email.split("@")[0])
                            .set(USERS.PASSWORD_HASH, "dev_bypass")
                            .set(USERS.SYSTEM_ROLE, (short) 0)
                            .set(USERS.CREATED_AT, LocalDateTime.now())
                            .returning()
                            .fetchOne();
                });

        // 2. Generate token and set HttpOnly Cookie
        String token = tokenProvider.createToken(user.getId());

        Cookie cookie = new Cookie(CookieAuthFilter.COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to false if testing on plain http://localhost
        cookie.setPath("/");
        cookie.setMaxAge(86400);
        response.addCookie(cookie);

        return ResponseEntity.ok(new AuthResponseDto("Dev Login successful", user.getId(), user.getUsername()));
    }
}