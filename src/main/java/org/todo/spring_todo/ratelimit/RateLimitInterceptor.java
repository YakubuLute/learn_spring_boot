package org.todo.spring_todo.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SESSION 5 — A HandlerInterceptor runs AFTER the security filters (so we know who the user is)
 * and BEFORE the controller method. Returning false or throwing stops the request.
 *
 * Request -> [Security filters] -> DispatcherServlet -> [THIS interceptor] -> Controller
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // One bucket per (client + endpoint). In-memory: fine for one server, see Session 5 notes for production.
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final RateLimitProperties properties;

    public RateLimitInterceptor(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.enabled() || !(handler instanceof HandlerMethod method)) {
            return true;
        }

        RateLimited limit = findLimit(method);
        if (limit == null) {
            return true;   // endpoint has no @RateLimited -> not limited
        }

        String key = clientId(request) + ":" + method.getMethod().toGenericString();
        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(limit.requests(), limit.perSeconds(), System::nanoTime));

        boolean allowed = bucket.tryConsume();
        // Friendly headers so clients can slow down BEFORE they hit the wall.
        response.setHeader("X-RateLimit-Limit", String.valueOf(bucket.capacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.availableTokens()));

        if (!allowed) {
            throw new RateLimitExceededException(bucket.secondsUntilNextToken());   // -> GlobalExceptionHandler -> 429
        }
        return true;
    }

    /** Method annotation wins; otherwise fall back to the class annotation. */
    private RateLimited findLimit(HandlerMethod method) {
        RateLimited onMethod = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RateLimited.class);
        if (onMethod != null) {
            return onMethod;
        }
        return AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RateLimited.class);
    }

    /** Logged-in users are limited by username; anonymous callers by IP address. */
    private String clientId(HttpServletRequest request) {
        Principal user = request.getUserPrincipal();
        return user != null ? "user:" + user.getName() : "ip:" + request.getRemoteAddr();
    }
}
