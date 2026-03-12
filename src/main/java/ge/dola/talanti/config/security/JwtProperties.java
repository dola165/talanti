package ge.dola.talanti.config.security;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Getter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private final String issuer;
    private final String audience;
    private final String secret;
    private final String accessTokenTtl;   // e.g. "PT15M"
    private final String refreshTokenTtl;  // e.g. "P30D"

    private final CookieProps cookie = new CookieProps();

    public JwtProperties(
            @DefaultValue("talanti") String issuer,
            @DefaultValue("talanti-web") String audience,
            String secret,
            @DefaultValue("PT15M") String accessTokenTtl,
            @DefaultValue("P30D") String refreshTokenTtl) {
        this.issuer = issuer;
        this.audience = audience;
        this.secret = secret;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public static class CookieProps {
        private String refreshName = "tl_refresh";
        private String domain;
        private Boolean secure = Boolean.TRUE;
        private String sameSite = "Strict";

        public String getRefreshName() { return refreshName; }
        public void setRefreshName(String n) { this.refreshName = n; }
        public String getDomain() { return domain; }
        public void setDomain(String d) { this.domain = d; }
        public Boolean getSecure() { return secure; }
        public void setSecure(Boolean s) { this.secure = s; }
        public String getSameSite() { return sameSite; }
        public void setSameSite(String s) { this.sameSite = s; }
    }
}