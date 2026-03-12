package ge.dola.talanti.security;

import ge.dola.talanti.config.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static ge.dola.talanti.jooq.Tables.AUTH_TOKENS;

@Service
// REMOVED: @RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;
    private final DSLContext dsl;
    private final SecretKey key; // We will generate this ourselves in the constructor!

    // MANUAL CONSTRUCTOR
    public JwtService(JwtProperties props, DSLContext dsl) {
        this.props = props;
        this.dsl = dsl;
        // Decode the base64 secret from your properties and generate the HMAC key
        byte[] secret = Decoders.BASE64.decode(props.getSecret());
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public record TokenPair(String accessToken, Instant accessExpiresAt, String refreshToken, Instant refreshExpiresAt) {}

    public TokenPair generateTokens(Long userId, String emailOrUsername, List<String> authorities) {
        Instant now = Instant.now();

        // 1. Generate Short-Lived Access Token
        Instant accessExp = now.plus(Duration.parse(props.getAccessTokenTtl()));
        String accessToken = Jwts.builder()
                .issuer(props.getIssuer())
                .audience().add(props.getAudience()).and()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessExp))
                .claim("u", emailOrUsername)
                .claim("a", authorities)
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        // 2. Generate Long-Lived Refresh Token
        Instant refreshExp = now.plus(Duration.parse(props.getRefreshTokenTtl()));
        String refreshJti = UUID.randomUUID().toString();
        String refreshToken = Jwts.builder()
                .issuer(props.getIssuer())
                .audience().add(props.getAudience()).and()
                .id(refreshJti)
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(refreshExp))
                .claim("typ", "refresh")
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        // 3. Save the Refresh JTI to the database using JOOQ
        dsl.insertInto(AUTH_TOKENS)
                .set(AUTH_TOKENS.JTI, refreshJti)
                .set(AUTH_TOKENS.USER_ID, userId)
                .set(AUTH_TOKENS.EXPIRES_AT, LocalDateTime.ofInstant(refreshExp, ZoneId.systemDefault()))
                .set(AUTH_TOKENS.REVOKED, false)
                .execute();

        return new TokenPair(accessToken, accessExp, refreshToken, refreshExp);
    }

    // Parses and validates the signature of any token
    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.getIssuer())
                .build()
                .parseSignedClaims(token);
    }

    // Checks if a refresh token is still valid in the database
    public boolean isRefreshTokenValidAndNotRevoked(String jti) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(AUTH_TOKENS)
                        .where(AUTH_TOKENS.JTI.eq(jti))
                        .and(AUTH_TOKENS.REVOKED.eq(false))
        );
    }

    // Kills a token (e.g., when a user logs out)
    public void revokeRefreshToken(String jti) {
        dsl.update(AUTH_TOKENS)
                .set(AUTH_TOKENS.REVOKED, true)
                .where(AUTH_TOKENS.JTI.eq(jti))
                .execute();
    }
}