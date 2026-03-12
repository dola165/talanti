package ge.dola.talanti.security;

import ge.dola.talanti.config.security.JwtProperties;
import ge.dola.talanti.jooq.tables.records.UsersRecord;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final JwtProperties props;
    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Google usually provides the email as an attribute
        String email = oAuth2User.getAttribute("email");
        String googleId = oAuth2User.getName(); // Depending on config, this is usually the sub/id

        // 1. Ensure user exists in DB (using your existing logic)
        UsersRecord user = authService.processOAuthPostLogin(email, "google", googleId);

        // 2. Generate custom JWTs for the React frontend
        var tokens = jwtService.generateTokens(user.getId(), user.getEmail(), List.of("ROLE_USER"));

        // 3. Set the Refresh Token HttpOnly Cookie (matching your AuthController logic)
        long seconds = Math.max(0, tokens.refreshExpiresAt().getEpochSecond() - Instant.now().getEpochSecond());
        var cookieBuilder = ResponseCookie.from(props.getCookie().getRefreshName(), tokens.refreshToken())
                .httpOnly(true)
                .secure(Boolean.TRUE.equals(props.getCookie().getSecure()))
                .sameSite(props.getCookie().getSameSite() != null ? props.getCookie().getSameSite() : "Strict")
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(seconds));

        if (props.getCookie().getDomain() != null && !props.getCookie().getDomain().isEmpty()) {
            cookieBuilder.domain(props.getCookie().getDomain());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());

        // 4. Redirect back to React with the access token in the URL query parameters
        String targetUrl = "http://localhost:5173/oauth2/callback?token=" + tokens.accessToken();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}// Added the missing closing brace