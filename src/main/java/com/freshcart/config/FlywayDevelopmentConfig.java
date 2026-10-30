package com.freshcart.config;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Local/development bootstrap support for databases that predate Flyway.
 *
 * The existing FreshCart development database was originally created without
 * flyway_schema_history. In non-production profiles we therefore baseline a
 * non-empty schema at version 1, then let pending migrations (for example V2)
 * run normally. Production deliberately does not use this behavior.
 */
@Configuration
@Profile("!prod")
public class FlywayDevelopmentConfig {

    @Bean
    FlywayConfigurationCustomizer developmentFlywayBaseline() {
        return configuration -> {
            configuration.baselineOnMigrate(true);
            configuration.baselineVersion("1");
            configuration.baselineDescription("FreshCart pre-Flyway schema baseline");
            configuration.cleanDisabled(true);
        };
    }
}
