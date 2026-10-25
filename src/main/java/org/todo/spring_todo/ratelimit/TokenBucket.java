package org.todo.spring_todo.ratelimit;

import java.util.function.LongSupplier;

/**
 * SESSION 5 — The token bucket algorithm.
 *
 * Picture a bucket that holds at most {@code capacity} tokens. Every request takes one token.
 * Tokens drip back in at a steady rate. Empty bucket => request rejected (HTTP 429).
 * This allows short bursts (a full bucket) but limits the long-run average rate.
 */
public class TokenBucket {

    private final long capacity;
    private final double tokensPerNano;
    private final LongSupplier nanoClock;   // injected so tests can control time

    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(long capacity, long refillPeriodSeconds, LongSupplier nanoClock) {
        this.capacity = capacity;
        this.tokensPerNano = (double) capacity / (refillPeriodSeconds * 1_000_000_000L);
        this.nanoClock = nanoClock;
        this.tokens = capacity;                       // start full
        this.lastRefillNanos = nanoClock.getAsLong();
    }

    /** synchronized: two requests from the same user at the same instant must not both grab the last token. */
    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    public synchronized long availableTokens() {
        refill();
        return (long) tokens;
    }

    /** How long a rejected client should wait before one token is back (for the Retry-After header). */
    public synchronized long secondsUntilNextToken() {
        refill();
        if (tokens >= 1) {
            return 0;
        }
        double nanosNeeded = (1 - tokens) / tokensPerNano;
        return (long) Math.ceil(nanosNeeded / 1_000_000_000d);
    }

    public long capacity() {
        return capacity;
    }

    private void refill() {
        long now = nanoClock.getAsLong();
        double newTokens = (now - lastRefillNanos) * tokensPerNano;
        tokens = Math.min(capacity, tokens + newTokens);
        lastRefillNanos = now;
    }
}
