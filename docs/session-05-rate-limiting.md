# Session 5 — Rate Limiting (and writing your own annotation)

> **Big idea:** Every client gets a bucket of tokens. Each request costs one token, and tokens drip back
> in over time. Empty bucket → **429 Too Many Requests**.

⏱️ ~2.5 hours · Starting point: end of Session 4 · Ending point: `@RateLimited(requests = 5, perSeconds = 60)` works on any endpoint

> 🏷️ **Code snapshots:** `git switch -c my-session-05 session-05` gives you this session's starting code. `git diff session-05 session-06` shows everything this session adds.

## 🎯 Learning objectives

1. Explain *why* APIs rate-limit (abuse, brute force, fairness, cost).
2. Compare fixed window, sliding window and token bucket algorithms.
3. **Write a custom annotation** (`@Target`, `@Retention`) and read it at runtime. This closes the loop on Session 1.
4. Use a `HandlerInterceptor` and know where it sits relative to security filters.
5. Respond correctly: `429`, `Retry-After`, `X-RateLimit-*` headers.
6. Bind custom settings with `@ConfigurationProperties`.

---

## 🪝 Hook (5 min)

Run this "bot" against the public registration endpoint:

```bash
for i in $(seq 1 50); do
  curl -s -o /dev/null -w "%{http_code} " -X POST localhost:8080/api/auth/register \
       -H 'Content-Type: application/json' -d "{\"username\":\"bot$i\",\"password\":\"password$i\"}"
done
```

50 × `201`. *"A real bot does 50,000. What else could a script hammer?"* (Login → password guessing, search → expensive DB queries, create → filling the database.)

---

## 💡 Big idea (15 min)

**Analogy: the arcade token machine.** You start with 5 tokens. Each game costs one. The machine gives you a new
token every 12 seconds, but your pocket holds at most 5. You can play a quick burst of 5 games, then you're limited to one game every 12 seconds.

```
capacity = 5      refill = 5 tokens / 60 s  (1 every 12 s)

time 0s   [●●●●●]  request → 201   [●●●●○]
          [●●●●○]  request → 201   [●●●○○]   … burst of 5 OK …
          [○○○○○]  request → 429   Retry-After: 12
time 12s  [●○○○○]  request → 201   (one token dripped back)
```

| Algorithm | How it works | Weakness |
|-----------|--------------|----------|
| **Fixed window** | Count requests per clock minute; reset at :00 | 5 at 0:59 + 5 at 1:00 = 10 in two seconds |
| **Sliding window** | Count requests in the *last* 60 s | Must remember every timestamp |
| **Token bucket** ✅ | Tokens refill continuously, up to a cap | Allows bursts (usually a feature!) |

### Where does the check run?

```
Request ─► [Security filters] ─► DispatcherServlet ─► [HandlerInterceptor] ─► Controller method
           knows WHO you are                          knows WHICH method,
                                                      can read its annotations ✅
```

We use an **interceptor** because it runs *after* security (so we can limit per user) and it knows which
controller method will run (so we can read `@RateLimited` on it).

---

## 🛠️ Code-along

### Step 1: our own annotation (I do)

```java
package org.todo.spring_todo.ratelimit;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})   // where it may be placed: methods and classes
@Retention(RetentionPolicy.RUNTIME)                // keep it in the compiled class so we can read it while running
@Documented
public @interface RateLimited {
    int requests() default 10;
    int perSeconds() default 60;
}
```

> 🔮 **Predict:** We put `@RateLimited` on a method right now and restart. Does anything get limited?
> **No.** Remember Session 1: an annotation is just a label. We still need code that *reads* it.

### Step 2: the token bucket (We do)

Open [`ratelimit/TokenBucket.java`](../src/main/java/org/todo/spring_todo/ratelimit/TokenBucket.java) and read it together. Three ideas:

1. `refill()`: tokens to add = time passed × rate, capped at `capacity`.
2. `tryConsume()` is `synchronized`: two requests at the same instant must not both take the last token.
3. Time comes from an injected `LongSupplier`. **That's DI again**, so the test can fake time:

```java
AtomicLong fakeNanos = new AtomicLong(0);
TokenBucket bucket = new TokenBucket(3, 60, fakeNanos::get);
bucket.tryConsume(); bucket.tryConsume(); bucket.tryConsume();
assertThat(bucket.tryConsume()).isFalse();
fakeNanos.addAndGet(20_000_000_000L);           // "wait" 20 s in zero real time
assertThat(bucket.tryConsume()).isTrue();
```

Run `./mvnw test -Dtest=TokenBucketTest`.

### Step 3: the interceptor that reads the label (I do)

Walk through [`RateLimitInterceptor`](../src/main/java/org/todo/spring_todo/ratelimit/RateLimitInterceptor.java):

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!properties.enabled() || !(handler instanceof HandlerMethod method)) return true;

    RateLimited limit = findLimit(method);               // method annotation, else class annotation
    if (limit == null) return true;                      // no label → no limit

    String key = clientId(request) + ":" + method.getMethod().toGenericString();
    TokenBucket bucket = buckets.computeIfAbsent(key,
            k -> new TokenBucket(limit.requests(), limit.perSeconds(), System::nanoTime));

    boolean allowed = bucket.tryConsume();
    response.setHeader("X-RateLimit-Limit", String.valueOf(bucket.capacity()));
    response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.availableTokens()));
    if (!allowed) throw new RateLimitExceededException(bucket.secondsUntilNextToken());
    return true;
}
```

- **One bucket per client per endpoint.** Alice spamming `POST /api/todos` doesn't block Bob, or Alice's `GET`s.
- `clientId` = username if logged in, otherwise IP address (for public endpoints like register).
- We *throw* instead of writing the response by hand, so `GlobalExceptionHandler` builds the 429 (`Retry-After` header included). One place for all errors.

### Step 4: register the interceptor (We do)

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final RateLimitInterceptor rateLimitInterceptor;
    public WebConfig(RateLimitInterceptor rateLimitInterceptor) { this.rateLimitInterceptor = rateLimitInterceptor; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
    }
}
```

### Step 5: a settings switch with `@ConfigurationProperties`

```java
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(boolean enabled) { }
```

```properties
app.rate-limit.enabled=true
```

Add `@ConfigurationPropertiesScan` to `SpringTodoApplication` so Spring finds the record. Now you can turn the
feature off per environment without touching code (`--app.rate-limit.enabled=false`).

### Step 6: use the label (You do)

```java
@RestController
@RequestMapping("/api/todos")
@RateLimited(requests = 30, perSeconds = 60)        // default for every endpoint in this class
public class TodoController {

    @PostMapping
    @RateLimited(requests = 5, perSeconds = 60)     // stricter for create; method beats class
    public ResponseEntity<TodoResponse> create(...) { ... }
}
```

And on `AuthController.register`: `@RateLimited(requests = 3, perSeconds = 60)`.

---

## ✅ Checkpoint: the interactive part

```bash
for i in $(seq 1 7); do
  curl -s -o /dev/null -w "%{http_code} " -u alice:password123 -X POST localhost:8080/api/todos \
       -H 'Content-Type: application/json' -d "{\"title\":\"spam $i\"}"
done; echo
# 201 201 201 201 201 429 429

curl -i -u alice:password123 -X POST localhost:8080/api/todos -H 'Content-Type: application/json' -d '{"title":"x"}'
# HTTP/1.1 429
# X-RateLimit-Limit: 5
# X-RateLimit-Remaining: 0
# Retry-After: 9
# {"status":429,"title":"Too Many Requests","detail":"Too many requests. Try again in 9 seconds."}
```

Now, **as Bob**, create a todo immediately: `201`. Separate buckets! Wait the `Retry-After` seconds and Alice gets exactly one more `201`.
Re-run the bot from the Hook: only 3 × `201`, then `429`.

Tests: `./mvnw test -Dtest='TokenBucketTest,RateLimitIntegrationTest'`.

---

## 💥 Break it on purpose

| Change | What you'll see | Lesson |
|--------|-----------------|--------|
| `@Retention(RetentionPolicy.CLASS)` | **No error, no limit.** Everything returns 201 forever. | `CLASS` retention throws the label away at runtime. The scariest bugs are silent ones. |
| Remove `addInterceptors` from `WebConfig` | No limit | A bean that nobody calls does nothing |
| Remove `synchronized` from `tryConsume` + fire 100 parallel requests (`xargs -P 20`) | Sometimes more than 5 get through | Shared state + concurrent requests = race condition |
| Remove `@ConfigurationPropertiesScan` | Startup error: *required a bean of type RateLimitProperties* | `@ConfigurationProperties` records must be registered |
| Use `request.getRemoteAddr()` only (no username) behind an office NAT | Whole office shares one bucket | Choose the key carefully |

---

## 🧪 Exercises

- 🟢 Limit `GET /api/todos/search` to 10 per minute.
- 🟢 Add `app.rate-limit.enabled=false` to a test profile and explain why tests might want that.
- 🟡 **Admins get double**: if the user has `ROLE_ADMIN`, use `limit.requests() * 2`. (Hint: `request.isUserInRole("ADMIN")`.)
- 🟡 **Memory leak!** The `buckets` map grows forever (one entry per IP ever seen). Remove buckets unused for 10 minutes (store last-access time, or use the Caffeine cache library with `expireAfterAccess`).
- 🔴 GraphQL (`/graphql`) is not limited: its handler isn't a `HandlerMethod`. Add a branch that limits `/graphql` per user (e.g., 60/min).

---

## ❓ Quiz

1. What HTTP status code and header tell a client "slow down, retry in N seconds"?
2. Why does the interceptor run *after* the security filters, and why does that matter here?
3. What does `@Retention(RetentionPolicy.RUNTIME)` do, and what happens without it?
4. Our buckets live in a `ConcurrentHashMap`. What breaks when we run **three** copies of the app behind a load balancer?
5. Token bucket with capacity 5, refill 5/minute. A client sends 5 requests at once, then waits 24 s. How many more can it send right away?

<details><summary>Answers</summary>

1. `429 Too Many Requests` + `Retry-After: N`.
2. Security runs first, so the user is known and we can limit per user, not just per IP.
3. Keeps the annotation available via reflection at runtime. Without it (default `CLASS`) the interceptor can't see it and nothing is limited, silently.
4. Each instance has its own buckets, so the real limit becomes 3× and depends on which server you hit. Fix: shared store (Redis + Bucket4j) or limit at an API gateway.
5. Two (one token every 12 s → 24 s = 2 tokens).
</details>

## 🧭 Recap

- Token bucket: bursts are allowed, the long-run rate is capped; reject with 429 + `Retry-After`.
- A custom annotation is just a label; an interceptor reads it and does the work. That's how Spring's own annotations work too.
- In-memory limiting is perfect for learning and single servers; production uses a shared store or gateway.

---

## 🎓 Facilitator notes

- This session is where annotations "click": learners realise `@GetMapping`, `@Transactional`, `@PreAuthorize` are all labels read by framework code like the interceptor they just wrote.
- **In production:** Bucket4j (+ Redis) for app-level limits, or the API gateway / NGINX / cloud load balancer. Resilience4j's `RateLimiter` limits *outgoing* calls, a common mix-up.
- `X-RateLimit-*` headers are a widespread convention, not a standard yet (an IETF draft proposes `RateLimit` / `RateLimit-Policy`).
- If the `for` loop is awkward on Windows, use the IntelliJ HTTP client or Postman's runner.
- Short on time: skip Step 5 (hard-code `enabled = true`) and do it as an exercise.
