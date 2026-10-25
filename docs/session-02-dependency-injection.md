# Session 2 — Dependency Injection (DI) & Inversion of Control (IoC)

> **Big idea:** Don't build your tools, *ask* for them. A class lists what it needs in its
> constructor, and Spring (the "container") builds everything and hands each piece over.

⏱️ ~3 hours · Starting point: end of Session 1 · Ending point: Controller → Service → Repository, a pluggable `Notifier`, a `Clock` bean, and the first unit test

> 🏷️ **Code snapshots:** `git switch -c my-session-02 session-02` gives you this session's starting code. `git diff session-02 session-03` shows everything this session adds.

## 🎯 Learning objectives

1. Explain *bean*, *container*, *IoC* and *DI* in plain words.
2. Use constructor injection (and say why it beats field injection).
3. Choose between `@Component`, `@Service`, `@Repository`, `@Controller`, and `@Bean`.
4. Resolve "two candidates" with `@Primary` and `@Qualifier`, and switch beans with `@Profile`.
5. Unit-test a service **without starting Spring**.

---

## 🪝 Hook (5 min)

Show the Session 1 controller and ask:

1. *"If the mobile team wants GraphQL next month, where does the 'create a todo' logic live?"* (Inside an HTTP class. We'd copy it.)
2. *"How would you test `create` without starting a web server?"* (You can't easily.)
3. *"What if we want to send a notification when a todo is created? Do we write `new EmailSender()` inside the controller?"*

Write on the board: **one class, one job**.

---

## 💡 Big idea (15 min)

**Analogy: the restaurant.** A chef doesn't drive to the farm, build an oven, and hire waiters before
cooking. The restaurant (the container) provides the oven and ingredients; the chef just says *"I need an oven
and tomatoes"*. If the restaurant swaps a gas oven for an electric one, the chef's recipe doesn't change.

| Term | Plain words |
|------|-------------|
| **Bean** | An object that Spring creates and manages. |
| **Container** (`ApplicationContext`) | The "restaurant": a big map of all beans, built at startup. |
| **IoC** (Inversion of Control) | *You* don't call `new`; the *framework* creates objects and calls your code. Control is inverted. |
| **DI** (Dependency Injection) | The specific way IoC gives a class its dependencies: through the constructor. |

Without DI:
```java
public class TodoService {
    private final TodoRepository repo = new TodoRepository();    // hard-wired, can't swap, can't fake
    private final EmailNotifier notifier = new EmailNotifier();  // what if email is down in tests?
}
```

With DI:
```java
@Service
public class TodoService {
    private final TodoRepository repo;
    private final Notifier notifier;                             // an INTERFACE: any implementation will do

    public TodoService(TodoRepository repo, Notifier notifier) { // "here's what I need"
        this.repo = repo;
        this.notifier = notifier;
    }
}
```

The layers we're building:

```
HTTP ──► TodoController ──► TodoService ──► TodoRepository ──► (memory today, database in Session 3)
         (web: JSON in/out)  (rules & logic)  (storage)
                                   └──────► Notifier (LogNotifier / EmailNotifier)
                                   └──────► Clock
```

### The stereotype annotations

All of these make a class a bean. The name tells *humans* (and some tools) what layer it is:

| Annotation | Layer | Extra behaviour |
|------------|-------|-----------------|
| `@Component` | generic | none |
| `@Service` | business logic | none (it's documentation) |
| `@Repository` | data access | translates database exceptions into Spring's `DataAccessException` |
| `@Controller` / `@RestController` | web | registers request mappings |
| `@Configuration` + `@Bean` | config | for objects from classes **you didn't write** (JDK, libraries) |

---

## 🛠️ Code-along

### Step 1: give the Todo a creation time

```java
public record Todo(Long id, String title, boolean completed, Instant createdAt) {
}
```

### Step 2: the repository, i.e. storage only (I do)

> ⭐ **Secret plan:** we deliberately use the *same method names* Spring Data JPA uses
> (`findAll`, `findById`, `save`, `deleteById`). In Session 3 we'll swap this class for a database
> and the service **won't change at all**.

```java
package org.todo.spring_todo.todo;

import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TodoRepository {

    private final Map<Long, Todo> store = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public List<Todo> findAll() {
        return List.copyOf(store.values());
    }

    public Optional<Todo> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Todo save(Todo todo) {
        Todo toStore = todo.id() == null
                ? new Todo(nextId.getAndIncrement(), todo.title(), todo.completed(), todo.createdAt())
                : todo;
        store.put(toStore.id(), toStore);
        return toStore;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }
}
```

### Step 3: the Notifier, one interface with two beans (We do)

Copy the three files from [`notification/`](../src/main/java/org/todo/spring_todo/notification):
`Notifier` (interface), `LogNotifier` (`@Component @Primary`), `EmailNotifier` (`@Component("emailNotifier")`).

> 🔮 **Predict:** Comment out `@Primary` on `LogNotifier` and start the app. What happens?
> A) Spring picks one at random  B) Spring injects both  C) The app refuses to start

Answer: **C**. You'll see something like:
```
Parameter 2 of constructor in TodoService required a single bean, but 2 were found:
    - emailNotifier
    - logNotifier
```
Spring won't guess. You must decide with `@Primary` (a default) or `@Qualifier` (pick by name at the injection point). Put `@Primary` back.

### Step 4: a `@Bean` for a class we didn't write (I do)

We can't put `@Component` on `java.time.Clock`; it's in the JDK. So we write a *factory method*:

```java
package org.todo.spring_todo.config;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
```

*Why inject a clock instead of calling `Instant.now()`?* Because in a test we can freeze time. You'll see the payoff in Step 7.

### Step 5: the service, i.e. the business logic (We do)

```java
package org.todo.spring_todo.todo;

@Service
public class TodoService {

    private final TodoRepository todoRepository;
    private final Notifier notifier;
    private final Clock clock;

    // Only one constructor → Spring uses it automatically. No @Autowired needed.
    public TodoService(TodoRepository todoRepository, Notifier notifier, Clock clock) {
        this.todoRepository = todoRepository;
        this.notifier = notifier;
        this.clock = clock;
    }

    public List<Todo> findAll() {
        return todoRepository.findAll();
    }

    public Todo findById(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    public Todo create(CreateTodoRequest request) {
        Todo saved = todoRepository.save(new Todo(null, request.title(), false, Instant.now(clock)));
        notifier.notify("someone", "Created todo: " + saved.title());   // real username in Session 4
        return saved;
    }

    public void delete(Long id) {
        findById(id);                    // throws 404 if missing
        todoRepository.deleteById(id);
    }
}
```

### Step 6: You do, slim the controller

Rewrite `TodoController` so it has **no `Map`** and only calls `TodoService`. Each method should be 1–3 lines.

<details><summary>Solution (excerpt)</summary>

```java
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public List<Todo> list() { return todoService.findAll(); }

    @GetMapping("/{id}")
    public Todo get(@PathVariable Long id) { return todoService.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Todo create(@Valid @RequestBody CreateTodoRequest request) { return todoService.create(request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { todoService.delete(id); }
}
```
</details>

### Step 7: the payoff, a unit test with no Spring at all

Because dependencies come in through the constructor, a test can pass in **fakes**:

```java
class TodoServiceTest {

    @Test
    void createUsesTheClockAndNotifies() {
        List<String> sent = new ArrayList<>();
        Clock frozen = Clock.fixed(Instant.parse("2026-01-01T09:00:00Z"), ZoneOffset.UTC);

        TodoService service = new TodoService(
                new TodoRepository(),                          // the real in-memory one
                (user, message) -> sent.add(message),          // a lambda IS a Notifier!
                frozen);

        Todo todo = service.create(new CreateTodoRequest("Buy milk"));

        assertThat(todo.createdAt()).isEqualTo(Instant.parse("2026-01-01T09:00:00Z"));
        assertThat(sent).containsExactly("Created todo: Buy milk");
    }
}
```

Run it. It takes **milliseconds**, because no server and no Spring context start. The finished repo's
[`TodoServiceTest`](../src/test/java/org/todo/spring_todo/todo/TodoServiceTest.java) uses Mockito for the repository; same idea.

### Step 8: see the container (make the hidden visible)

Temporarily add to `SpringTodoApplication`:

```java
@Bean
CommandLineRunner showBeans(ApplicationContext ctx) {
    return args -> Arrays.stream(ctx.getBeanDefinitionNames())
            .filter(name -> name.toLowerCase().contains("todo") || name.contains("otifier") || name.equals("clock"))
            .sorted()
            .forEach(name -> System.out.println("🫘 " + name + " → " + ctx.getBean(name).getClass().getSimpleName()));
}
```

> 🔮 **Predict:** How many `TodoService` objects exist? Call `ctx.getBean(TodoService.class)` twice and compare with `==`.
> (One. Beans are **singletons** by default, so every class that asks for a `TodoService` gets the same object.)

---

## ✅ Checkpoint

- `curl -X POST localhost:8080/api/todos -H 'Content-Type: application/json' -d '{"title":"DI rocks"}'` → 201, with `createdAt`.
- The console shows `[notify someone] Created todo: DI rocks` (from `LogNotifier`).
- `./mvnw test -Dtest=TodoServiceTest` is green.

---

## 💥 Break it on purpose

| Change | What you'll see | Lesson |
|--------|-----------------|--------|
| Remove `@Primary` from `LogNotifier` | *required a single bean, but 2 were found* | Ambiguity is an error, not a coin flip |
| Remove `@Service` from `TodoService` | *Parameter 0 of constructor in TodoController required a bean of type 'TodoService' that could not be found* | No label → not a bean → can't be injected |
| Open `spring_tut`'s `SoftwareEngineer`: it's a data class marked `@Component` with a constructor `(Integer id, String name, String techStack)` | *required a bean of type 'java.lang.Integer' that could not be found* | **Data objects are not beans.** Spring tried to inject an `Integer`! Remove `@Component`; use a record. |
| Make `TodoService` need `TodoController` in its constructor | *The dependencies of some of the beans form a cycle* | Layers go one way: controller → service → repository |
| Use field injection: `@Autowired private TodoRepository repo;` then `new TodoService()` in the test | `NullPointerException` | Field injection hides dependencies and breaks plain unit tests. Prefer constructors. |

---

## 🧪 Exercises

- 🟢 Switch `TodoService` to the email notifier **without removing `@Primary`**:
  `public TodoService(..., @Qualifier("emailNotifier") Notifier notifier, ...)`. Watch the 📧 log line.
- 🟢 Add `toggle(Long id)` to the service and `PATCH /api/todos/{id}/toggle` to the controller. It should notify only when a todo becomes completed.
- 🟡 Inject **all** notifiers: `List<Notifier> notifiers`, and call each one. (Spring injects every matching bean into a `List`.)
- 🟡 Put `@Profile("email")` on `EmailNotifier` and run with `-Dspring-boot.run.profiles=email`. What changes in the bean list (Step 8)?
- 🔴 Add `@Value("${app.greeting:Hello}") String greeting` to `HelloController`'s constructor, then set `app.greeting` in `application.properties`. Explain the `:Hello` part.

---

## ❓ Quiz

1. In one sentence: what is a bean?
2. Why is constructor injection preferred over `@Autowired` on fields? (Give two reasons.)
3. When do you use `@Bean` instead of `@Component`?
4. Two beans implement the same interface. Name two ways to tell Spring which one to inject.
5. By default, how many instances of a `@Service` does Spring create?

<details><summary>Answers</summary>

1. An object created and managed by the Spring container.
2. Dependencies are visible and can be `final`; you can create the class in a plain unit test with `new`; missing dependencies fail at startup, not with a `NullPointerException` later.
3. For classes you can't annotate (JDK/third-party), or when creation needs custom code.
4. `@Primary` on one bean (a default), or `@Qualifier("name")` at the injection point. (Also: `@Profile` to have only one active.)
5. One: the default scope is singleton.
</details>

## 🧭 Recap

- Classes declare what they need in the constructor; Spring builds and connects everything (IoC/DI).
- Controller (HTTP) → Service (logic) → Repository (storage): one job each.
- Depending on interfaces + injection = swappable parts and fast unit tests.

---

## 🎓 Facilitator notes

- The restaurant analogy works well; keep coming back to "who calls `new`?".
- Learners often ask *"where is `@Autowired`?"*. Since Spring 4.3, a single constructor is injected automatically. Show both forms once, then use only the constructor form.
- **Singleton + state = bug.** If time allows, add a `private int counter` to `TodoService`, increment it per request, and show two learners hitting the same server share the count. Lesson: beans should be stateless.
- Lombok's `@RequiredArgsConstructor` can write the constructor for you. Show it *after* they've written constructors by hand at least twice.
- The 💥 `spring_tut` example is gold: it's a mistake the learners have seen in a real starter.
