package org.todo.spring_todo.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/** SESSION 5 — We control "time" with a fake clock, so the test is instant and repeatable. */
class TokenBucketTest {

    private static final long ONE_SECOND = 1_000_000_000L;

    private final AtomicLong fakeNanos = new AtomicLong(0);

    @Test
    void allowsBurstUpToCapacityThenRejects() {
        TokenBucket bucket = new TokenBucket(3, 60, fakeNanos::get);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();   // bucket empty
    }

    @Test
    void refillsOverTime() {
        TokenBucket bucket = new TokenBucket(3, 60, fakeNanos::get);   // 1 token every 20s
        bucket.tryConsume();
        bucket.tryConsume();
        bucket.tryConsume();

        assertThat(bucket.secondsUntilNextToken()).isEqualTo(20);

        fakeNanos.addAndGet(20 * ONE_SECOND);
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();
    }

    @Test
    void neverHoldsMoreThanCapacity() {
        TokenBucket bucket = new TokenBucket(3, 60, fakeNanos::get);
        fakeNanos.addAndGet(3600 * ONE_SECOND);   // an hour passes

        assertThat(bucket.availableTokens()).isEqualTo(3);
    }
}
