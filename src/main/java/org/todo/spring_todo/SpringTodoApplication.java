package org.todo.spring_todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SESSION 1 — @SpringBootApplication is three annotations in one:
 *   @SpringBootConfiguration  -> this class can declare @Bean methods
 *   @EnableAutoConfiguration  -> "look at my dependencies and configure sensible defaults"
 *   @ComponentScan            -> find @Component/@Service/@Controller... in THIS package and below
 */
@SpringBootApplication
public class SpringTodoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringTodoApplication.class, args);
    }
}
