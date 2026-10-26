# Core Concepts Walkthrough: Bean, IoC, DI, AOP, Data Access

A short, self-contained lesson on the five ideas everything in Spring is built on. Every term points to real
code in this project, so you can show it live.

⏱️ ~60–75 minutes · Best used after Session 2 (DI), revisited after Session 3 (`@Transactional`) and Session 4 (`@PreAuthorize`)

> **Teaching order:** Bean → IoC → DI → AOP → Data Access. Each idea needs the one before it:
> you can't explain IoC without knowing what a bean is, and AOP only makes sense once learners know
> Spring hands out the objects.

> **About "DAF":** this is not a standard Spring acronym. Here it means **Data Access Framework**,
> Spring's data access support. If your curriculum uses it for something else (e.g. DAO, Data Access Object),
> adapt section 5.

---

## 1. Bean

> **One sentence:** A bean is an object that Spring creates, configures and manages for you.

**Analogy:** Staff at a restaurant are hired and trained by the restaurant. A customer who walks in from the
street is not staff. Beans are the restaurant's staff.

**In our code:** anything with `@Component`, `@Service`, `@Repository` or `@RestController`, or anything returned
by a `@Bean` method:

```java
@Service public class TodoService { ... }          // bean, found by scanning

@Bean public Clock clock() {                        // bean, created by a factory method
    return Clock.systemUTC();                       // (we can't put @Component on a JDK class)
}
```

See [`TodoService`](../src/main/java/org/todo/spring_todo/todo/TodoService.java) and
[`AppConfig`](../src/main/java/org/todo/spring_todo/config/AppConfig.java).

**What is *not* a bean:** a `Todo`, a `CreateTodoRequest`, a `TodoResponse`. These are data. You create thousands
of them with `new`, one per request or row.

**Key facts for learners:**

- Beans are **singletons** by default: one `TodoService` object serves every request.
- So beans should be **stateless**: no per-user fields inside a service.

> 🔮 **Ask the room:** Is `AppUser` a bean? *(No. It's an entity, i.e. data.)*

**Classic mistake:** `spring_tut`'s `SoftwareEngineer` is a data class marked `@Component`. Spring tries to create
it as a bean, can't find an `Integer` to pass to its constructor, and the app won't start.

---

## 2. IoC: Inversion of Control

> **One sentence:** Your code no longer controls creating objects and calling the main flow; the framework does,
> and it calls *your* code.

**Analogy:** Cooking at home vs. being a chef in a restaurant.

- **At home (normal control):** you buy ingredients, build the stove, cook, serve. You do everything.
- **In a restaurant (inverted control):** the restaurant hires you, gives you the kitchen, and *calls you* when an
  order comes in. You just cook.

**What gets inverted:**

| Without IoC | With IoC |
|-------------|----------|
| `main()` calls `new TodoRepository()`, `new TodoService(...)` and so on | `SpringApplication.run(...)` builds everything |
| You write a loop that reads HTTP requests | Spring calls `TodoController.create()` when `POST /api/todos` arrives |
| You decide when objects live and die | The **IoC container** (`ApplicationContext`) does |

**The Hollywood Principle:** *"Don't call us, we'll call you."* Our `TodoController` is never called by our own
code. Search the project for a caller of the controller's `create` method: there isn't one. Spring calls it.

**Show it live:** add this temporary runner to `SpringTodoApplication` to see what's inside the container:

```java
@Bean
CommandLineRunner showBeans(ApplicationContext ctx) {
    return args -> Arrays.stream(ctx.getBeanDefinitionNames())
            .filter(name -> name.toLowerCase().contains("todo") || name.contains("otifier") || name.equals("clock"))
            .sorted()
            .forEach(name -> System.out.println("🫘 " + name + " → " + ctx.getBean(name).getClass().getSimpleName()));
}
```

---

## 3. DI: Dependency Injection

> **One sentence:** DI is *how* IoC hands a class the objects it needs: the class lists them in its
> constructor, and Spring passes them in.

IoC is the general principle ("the framework is in charge"); DI is the specific technique ("the framework supplies
your dependencies").

**Before / after:**

```java
// ❌ The class builds its own dependencies: hard-wired, untestable
public class TodoService {
    private final TodoRepository repo = new TodoRepository();
    private final Notifier notifier = new EmailNotifier();
}

// ✅ The class asks for them: Spring injects them
@Service
public class TodoService {
    public TodoService(TodoRepository repo, Notifier notifier, Clock clock) { ... }
}
```

**Three ways to inject (teach the first, mention the others):**

| Style | Verdict |
|-------|---------|
| **Constructor** | ✅ Use this. Dependencies are visible, can be `final`, and the class works with `new` in tests. |
| Setter (`@Autowired` on a setter) | Rare: only for optional dependencies |
| Field (`@Autowired` on a field) | ❌ Hidden dependencies; `NullPointerException` in plain unit tests |

**Why it matters, shown in our code:**

1. **Swappable:** `Notifier` has two beans ([`notification/`](../src/main/java/org/todo/spring_todo/notification)).
   `@Primary` picks the default; `@Qualifier("emailNotifier")` picks the other. `TodoService` doesn't change.
2. **Testable:** [`TodoServiceTest`](../src/test/java/org/todo/spring_todo/todo/TodoServiceTest.java) builds the
   service with `new`, a lambda as the notifier, and a frozen `Clock`. No Spring, and it runs in milliseconds.
3. **Reusable:** REST (`TodoController`) and GraphQL (`TodoGraphQlController`) both receive the *same*
   `TodoService` bean.

> 🔮 **Ask the room:** Two beans implement `Notifier` and neither is `@Primary`. What happens?
> *(The app refuses to start: "required a single bean, but 2 were found".)*

---

## 4. AOP: Aspect-Oriented Programming

> **One sentence:** AOP lets you add behaviour *around* methods (transactions, security, logging) without
> writing that code inside each method.

**Analogy:** Airport security. Every passenger goes through it, but no passenger carries their own scanner. It's
wrapped around the journey by someone else. AOP wraps your methods the same way.

**The problem it solves:** some concerns cut across many classes ("cross-cutting concerns"). Without AOP, every
service method would start like this:

```java
public Todo update(...) {
    checkUserIsAllowed();          // security   ┐
    beginTransaction();            // transaction├─ the same lines copied into 50 methods
    try { /* the real work */ commit(); }        │
    catch (Exception e) { rollback(); throw e; } ┘
}
```

**You've already been using AOP since Session 3.** These annotations in our code work through it:

| Annotation | Behaviour wrapped around the method |
|------------|-------------------------------------|
| `@Transactional` (`TodoService`) | begin a transaction → run → commit, or roll back on an exception |
| `@PreAuthorize("hasRole('ADMIN')")` (`findAllForAdmin`) | check the role → run, or throw `AccessDeniedException` |

**How it works: proxies.** Spring doesn't inject your real `TodoService`. It injects a **proxy**, a wrapper object
that looks identical:

```
TodoController ──► [ Proxy: start tx / check role ] ──► real TodoService.update() ──► [ commit ]
```

**Show it live:** print `todoService.getClass()` inside the controller. You'll see something like
`TodoService$$SpringCGLIB$$0`. That's the proxy.

**Vocabulary (keep it light for beginners):**

| Term | Meaning | Airport version |
|------|---------|-----------------|
| **Aspect** | The cross-cutting module (e.g. "transactions") | The security department |
| **Advice** | The code that runs (before / after / around) | The scan itself |
| **Pointcut** | *Which* methods it applies to | "All departing passengers" |
| **Join point** | One specific method call being intercepted | You, at the gate, right now |

**💥 The #1 AOP gotcha (great demo):** calling an annotated method **from inside the same class** skips the proxy.
For example, another `TodoService` method calling `this.findAllForAdmin()` bypasses `@PreAuthorize`. Only calls
that come *from outside* go through the wrapper.

**Link to Session 5:** our rate limiter uses a `HandlerInterceptor`, which is the same "wrap around" idea for web
requests only. 🔴 Exercise: rewrite it as a real `@Aspect` with `@Around("@annotation(rateLimited)")`. That
needs the AspectJ starter (`spring-boot-starter-aspectj` in Boot 4, formerly `spring-boot-starter-aop`; check
against your Boot version).

---

## 5. DAF: Data Access Framework (Spring's data access support)

> **One sentence:** Spring gives you one consistent way to talk to databases, whatever the underlying
> technology, so you write less plumbing and handle errors the same way everywhere.

**Analogy:** A universal travel adapter. Plain JDBC, JPA/Hibernate and MongoDB each have their own "plug shape"
(APIs, exceptions). Spring's data access layer is the adapter: your code always plugs into the same shape.

**What it gives you, in layers from low to high:**

| Layer | What it does | In our course |
|-------|--------------|---------------|
| **Consistent exceptions** | Database-specific errors (`SQLException`, Hibernate exceptions) are translated into one hierarchy: `DataAccessException`, e.g. `DataIntegrityViolationException` | This is the extra job `@Repository` does |
| **Templates** | `JdbcTemplate` / `JdbcClient` handle connections, statements and cleanup; you write only the SQL and the mapping | Not used. Good "how it looked before" demo |
| **Transaction abstraction** | One `@Transactional` works for JDBC, JPA, etc. | `TodoService` (implemented with AOP, see #4) |
| **Spring Data repositories** | Write an interface; Spring generates the implementation, including queries from method names | [`TodoRepository`](../src/main/java/org/todo/spring_todo/todo/TodoRepository.java) `extends JpaRepository` |

**Why the consistent exceptions matter:** because `AppUser.username` is `unique = true`, inserting a duplicate
username throws a `DataIntegrityViolationException`. That's the same exception on H2 or PostgreSQL, so your error
handling doesn't change when the database does. (We check `existsByUsername` first, but the constraint is the
real safety net when two registrations race.)

**How it ties the other four together:** `TodoRepository` is a **bean** (1), created by the **IoC container** (2),
**injected** into `TodoService` (3), whose methods are wrapped in transactions by **AOP** (4), all on top of
Spring's **data access framework** (5).

---

## 🧭 The 30-second summary

| Term | One line |
|------|----------|
| **Bean** | An object Spring creates and manages |
| **IoC** | The framework is in charge of creating objects and calling your code |
| **DI** | Classes ask for what they need in the constructor; Spring provides it |
| **AOP** | Extra behaviour (transactions, security) wrapped around methods via proxies |
| **Data access framework** | One consistent way to use databases: shared exceptions, templates, transactions, repositories |

## ❓ Exit quiz

1. Is a `TodoResponse` a bean? Why or why not?
2. What's the difference between IoC and DI?
3. Why does `@Transactional` stop working when a method calls another method of the same class?
4. Name two annotations in our project that work through AOP.
5. What extra job does `@Repository` do that `@Component` doesn't?

<details><summary>Answers</summary>

1. No. It's data, created with `new` per response, not managed by the container.
2. IoC is the principle (the framework controls creation and flow); DI is the technique (dependencies are passed in).
3. The call doesn't go through the proxy, so no transaction logic runs.
4. `@Transactional` and `@PreAuthorize`.
5. It translates database-specific exceptions into Spring's `DataAccessException` hierarchy.
</details>
