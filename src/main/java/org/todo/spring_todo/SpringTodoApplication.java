package org.todo.spring_todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * SESSION 1 — @SpringBootApplication is three annotations in one:
 *   @SpringBootConfiguration  -> this class can declare @Bean methods
 *   @EnableAutoConfiguration  -> "look at my dependencies and configure sensible defaults"
 *   @ComponentScan            -> find @Component/@Service/@Controller... in THIS package and below
 */
@SpringBootApplication
@ConfigurationPropertiesScan   // SESSION 5: registers @ConfigurationProperties records like RateLimitProperties
public class SpringTodoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringTodoApplication.class, args);
    }
}
