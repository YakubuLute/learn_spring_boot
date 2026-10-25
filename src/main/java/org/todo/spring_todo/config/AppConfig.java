package org.todo.spring_todo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * SESSION 2 — @Bean is how you register objects from classes you can't annotate
 * (JDK classes, third-party libraries). The method's return value becomes a bean.
 */
@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        // Injecting a Clock (instead of calling Instant.now() everywhere) lets tests freeze time.
        return Clock.systemUTC();
    }
}
