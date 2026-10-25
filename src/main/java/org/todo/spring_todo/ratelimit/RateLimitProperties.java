package org.todo.spring_todo.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds every "app.rate-limit.*" entry in application.properties to this record.
 * app.rate-limit.enabled=false  ->  enabled() == false
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(boolean enabled) {
}
