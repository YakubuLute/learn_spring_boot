# Session 1 — Annotations & Your First REST API

> **Big idea:** An annotation is a sticky label on your code. On its own it does nothing.
> Spring *reads* the labels when the app starts and does the work for you.

⏱️ ~2.5 hours · Starting point: the `spring_todo` starter · Ending point: an in-memory Todo REST API

> 🏷️ **Code snapshots:** `git switch -c my-session-01 session-01` gives you this session's starting code. `git diff session-01 session-02` shows everything this session adds.

## 🎯 Learning objectives

By the end, learners can:

1. Explain what `@SpringBootApplication` does (it's three annotations in one).
2. Build `GET`, `POST`, `PUT`, `DELETE` endpoints with `@RestController` and `@*Mapping`.
3. Tell `@PathVariable`, `@RequestParam` and `@RequestBody` apart.
4. Return the right status code (200, 201, 204, 400, 404).
5. Validate input with `@Valid` + `@NotBlank`, and turn exceptions into clean JSON errors.

---

## 🪝 Hook (5 min)

Ask the room: *"To answer an HTTP request in plain Java, what would you need to write?"*
Collect answers: open a socket, parse the request line, parse headers, read the body, turn JSON into
objects, pick the right method, turn the result back into JSON, write the status line…

Then show this and say "by the end of today you'll understand every word of it":

```java
@RestController
class HelloController {
    @GetMapping("/hello")
    String hello() { return "Hello, Spring!"; }
}
```

---

## 💡 Big idea: annotations are labels (10 min)

**Analogy:** airport luggage tags. The tag (`FRAGILE`, `PRIORITY`, `LHR`) doesn't move the bag.
The airport's *system* reads the tag and does the right thing. `@GetMapping("/hello")` is a tag that
says "send GET /hello here". Spring is the airport system.

```
Your code + annotations ──► Spring reads them at startup ──► builds objects, wires routes, configures things
```

### Unpacking `@SpringBootApplication`

Open `SpringTodoApplication.java`. That one annotation is really three:

| Annotation | What it tells Spring |
|------------|----------------------|
| `@SpringBootConfiguration` | "This class can define beans (`@Bean` methods)." |
| `@EnableAutoConfiguration` | "Look at my dependencies and configure sensible defaults." Web starter on the classpath → start Tomcat on port 8080. |
| `@ComponentScan` | "Scan **this package and its sub-packages** for classes labelled `@Component`, `@Service`, `@RestController`…" |

> 🔮 **Predict:** If you create a controller in package `org.todo.other` (a *sibling*, not a child, of
> `org.todo.spring_todo`), will its endpoint work? (Answer in 💥 below.)

---

## 🛠️ Code-along

### Step 0: run the starter and meet the security guard

```bash
./mvnw spring-boot:run
```

> 🔮 **Predict:** We have no controllers yet. What will `curl -i http://localhost:8080/anything` return?

You'll get **401 Unauthorized**, not 404. Look in the console for:

```
Using generated security password: 3f1c...
```

The starter includes `spring-boot-starter-security`, and with **auto-configuration** its mere presence
locks every URL. That's `@EnableAutoConfiguration` at work! We'll learn security properly in Session 4.
For now, open `pom.xml` and **comment out** the security dependency:

```xml
<!--
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
-->
```

Reload Maven in your IDE, restart, and you get **404**, which is correct because there's no endpoint yet.

### Step 1: Hello controller (I do)

Create `src/main/java/org/todo/spring_todo/HelloController.java`:

```java
package org.todo.spring_todo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController                       // "I handle HTTP and my return values ARE the response body"
public class HelloController {

    @GetMapping("/hello")             // GET /hello  →  this method
    public String hello(@RequestParam(defaultValue = "Spring") String name) {
        return "Hello, " + name + "!";
    }
}
```

```bash
curl http://localhost:8080/hello
curl "http://localhost:8080/hello?name=Ama"
```

> 💡 `@RestController` = `@Controller` + `@ResponseBody`. Without `@ResponseBody`, Spring would look
> for an HTML template called `Hello, Spring!`.

### Step 2: the Todo model (We do)

Create package `todo` and a **record**, a compact Java class for data:

```java
package org.todo.spring_todo.todo;

public record Todo(Long id, String title, boolean completed) {
}
```

### Step 3: an in-memory TodoController (I do → We do)

```java
package org.todo.spring_todo.todo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/todos")            // prefix for every route in this class
public class TodoController {

    // Temporary "database". Session 2 moves this out; Session 3 replaces it with a real DB.
    private final Map<Long, Todo> todos = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @GetMapping                                           // GET /api/todos
    public List<Todo> list() {
        return List.copyOf(todos.values());
    }

    @GetMapping("/{id}")                                  // GET /api/todos/7
    public ResponseEntity<Todo> get(@PathVariable Long id) {
        Todo todo = todos.get(id);
        if (todo == null) {
            return ResponseEntity.notFound().build();     // 404
        }
        return ResponseEntity.ok(todo);                   // 200 + body
    }

    @PostMapping                                          // POST /api/todos  {"title":"Buy milk"}
    @ResponseStatus(HttpStatus.CREATED)                   // 201
    public Todo create(@RequestBody CreateTodoRequest request) {
        long id = nextId.getAndIncrement();
        Todo todo = new Todo(id, request.title(), false);
        todos.put(id, todo);
        return todo;
    }
}
```

And the request DTO (the shape of JSON the client sends):

```java
package org.todo.spring_todo.todo;

public record CreateTodoRequest(String title) {
}
```

> 🔮 **Predict:** Why don't we accept a full `Todo` in `create`? (Hint: who should decide the `id`?)

### Step 4: You do — DELETE

Learners write `DELETE /api/todos/{id}` alone. It should return **204 No Content** if deleted, or **404** if not found.

<details><summary>Solution</summary>

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    return todos.remove(id) != null
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
}
```
</details>

### The three ways data arrives in a request

| Annotation | Where the value lives | Example |
|------------|-----------------------|---------|
| `@PathVariable` | **inside** the path, identifies *which* resource | `/api/todos/`**`7`** |
| `@RequestParam` | after `?`, filters or options | `/api/todos?completed=`**`true`** |
| `@RequestBody` | the JSON body (POST/PUT/PATCH) | `{"title": "Buy milk"}` |

### Step 5: validation

Right now `{"title": ""}` creates a todo with an empty title. Let's add rules.
Change the DTO:

```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTodoRequest(
        @NotBlank(message = "title is required")
        @Size(max = 100, message = "title must be at most 100 characters")
        String title) {
}
```

And ask Spring to check it with `@Valid`:

```java
public Todo create(@Valid @RequestBody CreateTodoRequest request) {
```

> 🔮 **Predict:** We added `@NotBlank` but **forgot** `@Valid`. Does validation happen?
> (No. The rules are just labels until someone reads them. `@Valid` tells Spring to read them.)

### Step 6: clean error responses with `@RestControllerAdvice`

Copy [`common/GlobalExceptionHandler.java`](../src/main/java/org/todo/spring_todo/common/GlobalExceptionHandler.java)
(just the validation handler for now). It catches exceptions from **any** controller and turns them
into standard JSON (`ProblemDetail`, RFC 9457):

```json
{ "status": 400, "title": "Bad Request", "detail": "Validation failed",
  "errors": { "title": "title is required" } }
```

---

## ✅ Checkpoint

```bash
curl -i -X POST localhost:8080/api/todos -H 'Content-Type: application/json' -d '{"title":"Buy milk"}'
# → HTTP/1.1 201, body {"id":1,"title":"Buy milk","completed":false}

curl localhost:8080/api/todos/1          # → 200
curl -i localhost:8080/api/todos/99      # → 404
curl -i -X POST localhost:8080/api/todos -H 'Content-Type: application/json' -d '{"title":""}'
# → 400 with "errors": {"title": "title is required"}
curl -i -X DELETE localhost:8080/api/todos/1   # → 204
```

---

## 💥 Break it on purpose

| Change | Predict, then run. What you'll see | Lesson |
|--------|------------------------------------|--------|
| Remove `@RestController` | 404 on every todo route | No label → Spring never registers the class |
| Change it to `@Controller` | 500, *"Circular view path"* or template error | `@Controller` returns *view names*; `@RestController` returns *data* |
| Remove `@RequestBody` | `title` is `null` | Spring looks for `title` in the query string, not the JSON body |
| Move `HelloController` to package `org.todo.other` | 404 | `@ComponentScan` only looks **below** the main class's package |
| Two methods with `@GetMapping("/{id}")` | App fails: *Ambiguous mapping* | Each method + path combination must be unique |
| Send `{"title": "x"` (broken JSON) | 400 *JSON parse error* | Spring rejects bad input before your code runs |

---

## 🧪 Exercises

- 🟢 **PUT** `/api/todos/{id}` that replaces the title and `completed` flag (404 if missing).
- 🟢 **Filter**: `GET /api/todos?completed=true` using `@RequestParam(required = false) Boolean completed`.
- 🟡 **PATCH** `/api/todos/{id}/toggle` that flips `completed`.
- 🟡 Make the 404 come from the advice: throw a `TodoNotFoundException` and handle it in `GlobalExceptionHandler`
  (see the finished version in the repo).
- 🔴 `POST` should return a `Location: /api/todos/{id}` header. Use `ResponseEntity.created(uri)` and
  `ServletUriComponentsBuilder` (look at `TodoController.create` in the repo *after* trying).

---

## ❓ Quiz

1. Name the three annotations inside `@SpringBootApplication`.
2. `GET /api/todos/5?details=true`: which part is a `@PathVariable` and which a `@RequestParam`?
3. What status code should a successful `POST` that creates something return? And a successful `DELETE`?
4. You added `@NotBlank` but empty titles are still accepted. What's missing?
5. What's the difference between `@Controller` and `@RestController`?

<details><summary>Answers</summary>

1. `@SpringBootConfiguration`, `@EnableAutoConfiguration`, `@ComponentScan`.
2. `5` is the path variable; `details=true` is the request param.
3. 201 Created; 204 No Content.
4. `@Valid` on the `@RequestBody` parameter.
5. `@RestController` adds `@ResponseBody`: return values are written as the body (JSON) instead of being treated as view/template names.
</details>

## 🧭 Recap

- Annotations are labels; Spring reads them at startup and acts on them.
- A controller maps HTTP (method + path) to Java methods; data arrives via path, query or body.
- Validate at the edge with `@Valid`; turn exceptions into clean errors in one `@RestControllerAdvice`.

**Notice the smell:** the controller stores data *and* handles HTTP. Session 2 fixes that.

---

## 🎓 Facilitator notes

- **Biggest stumbling block:** the 401 in Step 0. Make it a *feature* of the lesson, because it's the first proof that auto-configuration exists.
- Learners on Windows: use Git Bash for `curl`, or the IntelliJ HTTP client. Single quotes in `-d '{...}'` don't work in `cmd.exe`.
- If someone asks "why records?": records are immutable data carriers. DTOs are a perfect fit. Entities (Session 3) can't be records.
- `ConcurrentHashMap` / `AtomicLong`: briefly say "web servers handle many requests at once; these are thread-safe versions." Don't go deeper.
- Short on time? Skip Step 6; do it at the start of Session 2.
