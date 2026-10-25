package org.todo.spring_todo.ratelimit;

import java.lang.annotation.*;

/**
 * SESSION 5 — Our very own annotation! On its own it does NOTHING; it is just a label.
 * RateLimitInterceptor reads it at runtime and enforces the limit.
 *
 * Example: @RateLimited(requests = 5, perSeconds = 60) = at most 5 calls per minute per user.
 */
@Target({ElementType.METHOD, ElementType.TYPE})   // allowed on methods and classes
@Retention(RetentionPolicy.RUNTIME)                // keep it in the .class file so we can read it while running
@Documented
public @interface RateLimited {

    int requests() default 10;

    int perSeconds() default 60;
}
