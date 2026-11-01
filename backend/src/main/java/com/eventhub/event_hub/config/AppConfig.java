package com.eventhub.event_hub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class AppConfig {

    /**
     * Single source of "now". Services never call LocalDateTime.now() directly, so business
     * rules that depend on time (past events, order expiry) are deterministic in tests.
     */
    @Bean
    public Clock clock(@Value("${app.timezone:UTC}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
