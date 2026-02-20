package ge.dola.talanti.security;

import ge.dola.talanti.jooq.tables.records.Oauth2LoginsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.OAUTH2_LOGINS;

@Repository
public class OAuth2LoginRepository {

    private final DSLContext dsl;

    public OAuth2LoginRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Oauth2LoginsRecord> findByProviderAndId(String provider, String providerId) {
        return dsl.selectFrom(OAUTH2_LOGINS)
                .where(OAUTH2_LOGINS.PROVIDER.eq(provider))
                .and(OAUTH2_LOGINS.PROVIDER_ID.eq(providerId))
                .fetchOptional();
    }
}