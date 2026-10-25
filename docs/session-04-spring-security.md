# Session 4 — Spring Security

> **Big idea:** Before any request reaches your controller, it walks through a **chain of filters**,
> like checkpoints at an airport. You configure the checkpoints; you don't write `if (loggedIn)` in every method.

⏱️ ~3 hours · Starting point: end of Session 3 · Ending point: users in the database, BCrypt passwords, each user sees only their own todos, an admin-only area

> 🏷️ **Code snapshots:** `git switch -c my-session-04 session-04` gives you this session's starting code. `git diff session-04 session-05` shows everything this session adds.

## 🎯 Learning objectives

1. Explain **authentication** (who are you?) vs **authorization** (what may you do?), and **401 vs 403**.
2. Describe the security **filter chain** and configure it with a `SecurityFilterChain` bean.
3. Store users in the database and plug them in with a `UserDetailsService`.
4. Hash passwords with BCrypt and explain why we never store plain text.
5. Protect by URL (`requestMatchers…hasRole`) and by method (`@PreAuthorize`).
6. Get the current user in a controller and make data **per-user**.

---

## 🪝 Hook (5 min)

```bash
curl localhost:8080/api/todos
```

*"Anyone on the internet can read, change and delete every todo. Who should be allowed to see your todos?"*

Then **uncomment** `spring-boot-starter-security` in `pom.xml` (we commented it out in Session 1) and restart.

> 🔮 **Predict:** What does the same curl return now?

`401`. Look for `Using generated security password: …` in the log. Try it:

```bash
curl -u user:<paste-generated-password> localhost:8080/api/todos
```

One dependency and everything is locked, which is *secure by default*. But there's one user with a random password. Today we make it real.

---

## 💡 Big idea (15 min)

**Analogy: the airport.**

| Airport | Spring Security |
|---------|-----------------|
| Passport check: *"Who are you?"* | **Authentication**. Fails with **401 Unauthorized** ("we don't know who you are") |
| Boarding gate: *"Is your ticket for this flight?"* | **Authorization**. Fails with **403 Forbidden** ("we know who you are, and the answer is no") |
| Series of checkpoints before the plane | The **filter chain** |
| Your passport | Credentials (here: HTTP Basic header) |
| Business-class lounge | `/api/admin/**`, needs role ADMIN |

```
Request
  │
  ▼  ┌──────────────────── Security filter chain ────────────────────┐
  ├─►│ … ─► BasicAuthenticationFilter ─► … ─► AuthorizationFilter ─► │──► DispatcherServlet ──► Controller
  │  │      "read Authorization header,        "does this user have    │
  │  │       check the password"                the needed role?"      │
  │  └─────────── fail? 401 ──────────────────── fail? 403 ───────────┘
```

**HTTP Basic** sends `Authorization: Basic base64(username:password)` on *every* request. Decode one together:

```bash
echo -n 'alice:password123' | base64          # YWxpY2U6cGFzc3dvcmQxMjM=
echo 'YWxpY2U6cGFzc3dvcmQxMjM=' | base64 -d   # alice:password123  ← anyone can do this!
```

> ⚠️ Base64 is **encoding, not encryption**. Basic auth is only acceptable over **HTTPS**.

---

## 🛠️ Code-along

### Step 1: the user entity & repository (We do)

Copy from [`user/`](../src/main/java/org/todo/spring_todo/user): `Role` (enum `USER, ADMIN`), `AppUser` (`@Entity`, table `app_user`), `AppUserRepository`:

```java
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

### Step 2: password hashing (I do, with a demo)

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

> 🔮 **Predict:** We encode `"password123"` twice. Are the two hashes equal?

Try it in a test or `jshell` (`new BCryptPasswordEncoder().encode("password123")`): **different every time**, e.g.
`$2a$10$N9qo8uLOickgx2ZMRZoMye…`, because BCrypt adds a random *salt*. `matches(raw, hash)` still returns `true`.
BCrypt is also **slow on purpose**, so an attacker who steals the table can't try billions of guesses per second.

### Step 3: teach Spring Security about our users

```java
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public DatabaseUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user " + username));
        return User.withUsername(user.getUsername())
                .password(user.getPassword())   // the HASH
                .roles(user.getRole().name())   // "ADMIN" → authority "ROLE_ADMIN"
                .build();
    }
}
```

We never compare passwords ourselves: Spring Security loads the user, then asks the `PasswordEncoder` to compare.
Because this bean exists, the "generated security password" disappears from the log (DI + auto-configuration again!).

### Step 4: the security rules (I do, line by line)

Open [`config/SecurityConfig.java`](../src/main/java/org/todo/spring_todo/config/SecurityConfig.java) and walk through the five numbered blocks:

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
        .requestMatchers("/h2-console/**", "/graphiql/**", "/error").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated())          // ← always last: "everything else needs login"
.httpBasic(Customizer.withDefaults())
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.csrf(csrf -> csrf.disable())
.headers(h -> h.frameOptions(f -> f.sameOrigin()));
```

> 🔮 **Predict:** Rules are checked **top to bottom, first match wins**. What happens if `.anyRequest().authenticated()` is first?
> (Spring refuses to start: nothing can come after `anyRequest()`.)

**Why disable CSRF?** CSRF attacks trick a *browser* into sending its cookies to your site. Our API uses no cookies
or sessions (stateless, credentials on every request), so there's nothing to steal. If you build a browser app with
login sessions, **keep CSRF on**.

### Step 5: registration (We do)

`POST /api/auth/register` is public. Copy `RegisterRequest`, `UserResponse`, `UserService`, `AuthController`.
Key lines in `UserService.register`:

```java
if (userRepository.existsByUsername(request.username())) {
    throw new UsernameTakenException(request.username());   // → 409 Conflict via GlobalExceptionHandler
}
String hash = passwordEncoder.encode(request.password());
return userRepository.save(new AppUser(request.username(), hash, Role.USER));
```

Note `UserResponse` has **no password field**. What you leave out of a DTO can never leak.

### Step 6: whose todo is it? (We do)

Add an owner to `Todo`:

```java
@ManyToOne(optional = false)
@JoinColumn(name = "owner_id")
private AppUser owner;
```

Ask Spring for the logged-in user by adding a `Principal` parameter to any controller method:

```java
@GetMapping
public List<TodoResponse> list(@RequestParam(required = false) Boolean completed, Principal principal) {
    return todoService.findAll(principal.getName(), completed).stream().map(TodoResponse::from).toList();
}
```

…and make every repository query owner-aware:

```java
List<Todo> findByOwnerUsernameOrderByCreatedAtDesc(String username);
Optional<Todo> findByIdAndOwnerUsername(Long id, String username);
```

> 🔮 **Predict:** Bob requests `GET /api/todos/1`, which is Alice's todo. Should he get **403** or **404**?
> We return **404**: a 403 would confirm "todo 1 exists, it's just not yours", which leaks information.

### Step 7: You do, the admin area

1. `AdminController` with `GET /api/admin/todos` returning *all* todos.
2. In `TodoService`, protect it a second time:

```java
@PreAuthorize("hasRole('ADMIN')")
public List<Todo> findAllForAdmin() { return todoRepository.findAll(); }
```

`@PreAuthorize` works because of `@EnableMethodSecurity` on `SecurityConfig`. It protects the method **wherever** it's called from,
including GraphQL in Session 6. URL rules + method rules = *defence in depth*.

### Step 8: demo data

Copy [`config/DataSeeder.java`](../src/main/java/org/todo/spring_todo/config/DataSeeder.java). It's a `CommandLineRunner` that creates
`alice / password123` (USER) and `admin / admin12345` (ADMIN) at startup. *Training only. Never hard-code real passwords.*

---

## ✅ Checkpoint

```bash
curl -i localhost:8080/api/todos                                       # 401
curl -i -u alice:wrong localhost:8080/api/todos                        # 401
curl -u alice:password123 localhost:8080/api/todos                     # only alice's todos
curl -i -u alice:password123 localhost:8080/api/admin/todos            # 403
curl -u admin:admin12345 localhost:8080/api/admin/todos                # everyone's todos
curl -i -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
     -d '{"username":"bob","password":"bobsecret1"}'                    # 201, no password in response
curl -u bob:bobsecret1 localhost:8080/api/auth/me                      # {"username":"bob","role":"USER",...}
curl -i -u bob:bobsecret1 localhost:8080/api/todos/1                   # 404, not bob's
```

In the H2 console: `SELECT * FROM APP_USER;` shows hashes starting with `$2a$10$`.

Run the security tests: `./mvnw test -Dtest=TodoApiSecurityTest`.

---

## 💥 Break it on purpose

| Change | What you'll see | Lesson |
|--------|-----------------|--------|
| Delete the `PasswordEncoder` bean | Login fails; log says *There is no PasswordEncoder mapped for the id "null"* | Spring won't guess how passwords are stored |
| Seed a user with a plain-text password (no `encode`) | 401 for that user | The stored value must be a hash from the same encoder |
| `.hasRole("ROLE_ADMIN")` | Startup error: *should not start with ROLE_* | `hasRole` adds the `ROLE_` prefix for you (`hasAuthority` doesn't) |
| Put `.anyRequest().authenticated()` first | Startup error: *Can't configure requestMatchers after anyRequest* | Order matters; most specific first |
| Remove `@EnableMethodSecurity` | Alice *can* reach `findAllForAdmin()` if called from elsewhere | Method annotations are labels; something must read them |
| Return `AppUser` entity from `/me` | The JSON contains the password hash 😱 | Always use DTOs at the edge |

---

## 🧪 Exercises

- 🟢 `GET /api/admin/users` (admin only) returning `List<UserResponse>`.
- 🟢 Write a MockMvc test: Bob cannot delete Alice's todo (expect 404). Use `.with(httpBasic("bob", "..."))`.
- 🟡 `PUT /api/auth/password` with `{ "currentPassword", "newPassword" }`: check the current one with `passwordEncoder.matches`.
- 🟡 Use `@AuthenticationPrincipal UserDetails user` instead of `Principal`. What extra information do you get?
- 🔴 Replace Basic auth with **JWT**: add `spring-boot-starter-oauth2-resource-server`, a `POST /api/auth/token` endpoint that issues a token with `JwtEncoder`, and `.oauth2ResourceServer(o -> o.jwt(...))`. Discuss: what problem does a token solve that Basic doesn't?

---

## ❓ Quiz

1. 401 or 403? (a) No `Authorization` header. (b) Alice calls an admin endpoint. (c) Wrong password.
2. Why does BCrypt produce a different hash each time for the same password, and how does login still work?
3. Is HTTP Basic auth safe over plain HTTP? Why?
4. What's the difference between `hasRole("ADMIN")` and `hasAuthority("ROLE_ADMIN")`?
5. Why return 404 instead of 403 when Bob asks for Alice's todo?

<details><summary>Answers</summary>

1. (a) 401 (b) 403 (c) 401.
2. A random salt is generated and stored *inside* the hash; `matches` reads the salt back out and re-hashes the attempt with it.
3. No. Base64 is reversible; anyone on the network can read the password. Use HTTPS.
4. Nothing in effect: `hasRole` adds the `ROLE_` prefix automatically.
5. To avoid revealing that the resource exists.
</details>

## 🧭 Recap

- Security is a filter chain in front of your controllers: authentication (401) then authorization (403).
- Users live in our DB; Spring Security reads them through `UserDetailsService` and checks BCrypt hashes.
- Protect URLs in `SecurityFilterChain`, methods with `@PreAuthorize`, and data by always filtering by owner.

---

## 🎓 Facilitator notes

- **Common confusion:** "I logged in, why do I need to send the password again?" That's stateless Basic auth: each request stands alone. This is the right moment to tease JWT (the 🔴 exercise).
- Some learners will try the browser: `/api/todos` shows the browser's native login popup because of the `WWW-Authenticate: Basic` header. That's fine and a nice visual.
- The H2 console needs `frameOptions().sameOrigin()`, and it's under `permitAll()` for convenience. Say clearly: **never expose the H2 console in production** (the `postgres` profile disables it).
- Adding `owner` to `Todo` breaks existing seed data/inserts; that's why the seeder is introduced in the same session. With `create-drop` the schema is rebuilt on each restart, so no migration is needed.
- If time is short: skip Step 7's `@PreAuthorize` and do it as an exercise.
