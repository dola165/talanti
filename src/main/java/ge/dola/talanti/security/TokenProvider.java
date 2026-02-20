package ge.dola.talanti.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class TokenProvider {

    private final SecretKey key;
    private final long tokenValidityInMilliseconds;

    // These values should come from your application.yml / application.properties
    public TokenProvider(
            @Value("${app.security.jwt.secret:SuperSecretKeyThatShouldBeAtLeast32CharactersLongAndInYourEnvVariables}") String secret,
            @Value("${app.security.jwt.expiration-ms:86400000}") long tokenValidityInMilliseconds) { // Default 1 day
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
    }

    // Create a token containing ONLY the User ID
    public String createToken(Long userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + this.tokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    // Extract the User ID from the token
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    // Verify the token hasn't been tampered with or expired
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("Invalid JWT token: " + e.getMessage());
            return false;
        }
    }
}