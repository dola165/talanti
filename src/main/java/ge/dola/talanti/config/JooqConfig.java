package ge.dola.talanti.config;

import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqConfig {

    @Bean
    public DefaultConfigurationCustomizer configurationCustomizer(PostgresRlsListener rlsListener) {
        return (DefaultConfiguration c) -> {
            c.set(rlsListener);
        };
    }
}