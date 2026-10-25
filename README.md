# Spring Boot Deep Dive — the Todo API

A six-session, hands-on course. Learners build **one** application, a multi-user Todo API,
and each session adds one big idea to it:

| # | Session | The big idea | Learners walk away able to… |
|---|---------|--------------|------------------------------|
| 1 | [Annotations & your first REST API](docs/session-01-annotations-and-rest.md) | Annotations are labels that Spring reads | build GET/POST/PUT/DELETE endpoints, validate input, return proper status codes |
| 2 | [Dependency Injection](docs/session-02-dependency-injection.md) | Don't build your tools, ask for them | split code into controller → service → repository, use `@Primary`, `@Qualifier`, `@Bean`, unit-test without Spring |
| 3 | [Databases with Spring Data JPA](docs/session-03-database-jpa.md) | Interfaces become SQL | map entities, write derived queries & JPQL, understand transactions, switch H2 ↔ PostgreSQL |
| 4 | [Spring Security](docs/session-04-spring-security.md) | Every request walks through a chain of checkpoints | store users with BCrypt, protect URLs and methods by role, explain 401 vs 403 |
| 5 | [Rate Limiting](docs/session-05-rate-limiting.md) | A bucket of tokens protects your API | write a custom annotation, a token bucket and an interceptor, return 429 correctly |
| 6 | [GraphQL](docs/session-06-graphql.md) | The client chooses the shape of the data | write a schema, queries, mutations and field resolvers that reuse the same service |

Extras:

- [Teaching playbook](docs/00-teaching-playbook.md): the teaching style used in every session. Read it before you teach.
- [Annotations cheat sheet](docs/annotations-cheatsheet.md): every annotation used in the course on one page.

> **Why this order?** Annotations and DI come first because every later topic (repositories,
> security config, GraphQL controllers) is a bean that gets injected and is configured with
> annotations. Once learners understand those two ideas, the database, security and GraphQL
> sessions feel like more of the same pattern.

---

## Running the finished app

Requirements: JDK 17+ (tested on JDK 27). You don't need to install Maven because the `./mvnw` wrapper downloads it.

```bash
./mvnw spring-boot:run
```

| What | Where |
|------|-------|
| REST API | http://localhost:8080/api/todos |
| H2 database console | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:tododb`, user `sa`, empty password) |
| GraphiQL playground | http://localhost:8080/graphiql |

Demo users (created by `DataSeeder`, **for training only**):

| Username | Password | Role |
|----------|----------|------|
| `alice` | `password123` | USER |
| `admin` | `admin12345` | ADMIN |

Smoke test:

```bash
curl -u alice:password123 http://localhost:8080/api/todos
```

Run the tests (16 tests: unit, security, rate limit, GraphQL):

```bash
./mvnw test
```

### Using PostgreSQL instead of H2 (optional, Session 3)

```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

---

## Session snapshots (git tags)

Every session has a tag pointing at its **starting** code, so a learner who fell behind (or missed a day) can jump in:

| Tag | Code at that point |
|-----|--------------------|
| `session-01` | The Spring Initializr starter (security on: everything returns 401) |
| `session-02` | End of S1: in-memory REST API with validation |
| `session-03` | End of S2: controller → service → in-memory repository, `Notifier` beans, `Clock` bean |
| `session-04` | End of S3: JPA entity + repository, H2 console, PostgreSQL profile |
| `session-05` | End of S4: users, BCrypt, security rules, admin endpoint |
| `session-06` | End of S5: `@RateLimited`, token bucket, 429 responses |
| `final` | End of S6: GraphQL. The complete solution (same as `main`) |

```bash
git tag                                   # list the snapshots
git switch -c my-work session-03          # start Session 3 on your own branch
git diff session-03 session-04            # everything Session 3 adds: a ready-made answer key
git switch main                           # back to the finished app
```

Every snapshot compiles and its tests pass. The `docs/` guides are the same in every snapshot. Links inside them
point to files in the **finished** code, so some won't exist yet in early snapshots
(use `git show final:<path>` to peek).

> In sessions 1–3 the security starter is commented out in `pom.xml` (see Session 1, Step 0), so the
> `session-02` to `session-04` snapshots run without login.

---

## Project map

```
src/main/java/org/todo/spring_todo
├── SpringTodoApplication.java        S1  entry point, @SpringBootApplication
├── todo/                              the main feature
│   ├── TodoController.java           S1  REST endpoints
│   ├── TodoService.java              S2  business logic (constructor injection)
│   ├── Todo.java, Priority.java      S3  JPA entity
│   ├── TodoRepository.java           S3  Spring Data repository
│   ├── AdminController.java          S4  admin-only endpoint
│   └── dto/                          S1  request/response records + validation
├── notification/                     S2  one interface, two beans (@Primary / @Qualifier)
├── user/                             S4  AppUser, registration, UserDetailsService
├── config/                           S2+ AppConfig (@Bean Clock), SecurityConfig, WebConfig, DataSeeder
├── ratelimit/                        S5  @RateLimited, TokenBucket, interceptor
├── graphql/                          S6  @QueryMapping / @MutationMapping controller
└── common/GlobalExceptionHandler     S1  exceptions → clean JSON errors
src/main/resources
├── application.properties            H2, GraphiQL, rate-limit switch
├── application-postgres.properties   S3  PostgreSQL profile
└── graphql/schema.graphqls           S6  the GraphQL contract
```

Look for `SESSION n` comments in the code. They point to where each idea is introduced.

## Notes on the starter project

- **Lombok and JDK 27:** Boot's default Lombok (1.18.46) crashes `javac` on JDK 27.
  `pom.xml` pins `<lombok.version>1.18.48</lombok.version>`. The `spring_tut` project needs the
  same line in its `<properties>` before it will compile.
- Compared to the generated starter, `pom.xml` swaps Spring Data **JDBC** for Spring Data **JPA** (the
  more common choice in industry and training), adds H2 + validation, and drops the OAuth2 client and
  GraphQL client-codegen plugin (they're not needed for a GraphQL *server*, and they confuse beginners).
