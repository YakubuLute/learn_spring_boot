# Session 6 — GraphQL with Spring for GraphQL

> **Big idea:** In REST, the *server* decides the shape of each response. In GraphQL, the *client* sends a
> query describing exactly the fields it wants, and gets back exactly that shape.

⏱️ ~3 hours · Starting point: end of Session 5 · Ending point: `/graphql` with queries, mutations and a computed field, all reusing `TodoService`

> 🏷️ **Code snapshots:** `git switch -c my-session-06 session-06` gives you this session's starting code. `git diff session-06 final` shows everything this session adds.

## 🎯 Learning objectives

1. Explain over-fetching and under-fetching, and when GraphQL helps (and when it doesn't).
2. Read and write a schema: `type`, `Query`, `Mutation`, `input`, `enum`, `!`, `[ ]`.
3. Write resolvers with `@QueryMapping`, `@MutationMapping`, `@Argument`, `@SchemaMapping`.
4. Use GraphiQL with authentication; use variables, aliases and fragments.
5. Handle errors the GraphQL way and spot the **N+1 problem**.

---

## 🪝 Hook (5 min)

Two clients, two problems:

- **Mobile app** shows only todo titles. `GET /api/todos` sends `id, title, description, completed, priority, createdAt, owner`
  for every todo, so the phone downloads 6 fields it throws away → **over-fetching**.
- **Dashboard** needs: my username + role, my todo count, and my open todos. That's `/api/auth/me` + `/api/todos?completed=false` + a count
  → three round-trips → **under-fetching**.

GraphQL answer, one request:

```graphql
{
  me { username role todoCount }
  todos(completed: false) { title }
}
```

---

## 💡 Big idea (15 min)

**Analogy: restaurant set menu vs. ordering à la carte.** REST endpoints are set menus: "Menu 3" always comes with
the same dishes. GraphQL is ordering à la carte from a **menu card (the schema)**: you list exactly the dishes you
want, and the kitchen can only make what's on the card.

```
POST /graphql   {"query": "{ todos { title } }"}
        │
        ▼
  Schema (schema.graphqls) ── is the query valid? (fields exist? types right?)
        │
        ▼
  Resolvers (@QueryMapping …)  ──►  TodoService  ──►  TodoRepository   ← same service as REST!
        │
        ▼
  {"data": {"todos": [{"title": "Buy milk"}]}}
```

| | REST | GraphQL |
|---|------|---------|
| Endpoints | Many (`/api/todos`, `/api/todos/{id}`…) | One (`POST /graphql`) |
| Response shape | Fixed by the server | Chosen by the client |
| Contract | OpenAPI (optional) | Schema (required, typed) |
| Errors | HTTP status codes | Usually `200` + an `errors` array |
| HTTP caching | Easy (GET + URLs) | Harder |
| Best for | Simple CRUD, public APIs, file downloads | Many different clients, nested/related data |

---

## 🛠️ Code-along

### Step 1: the schema is the contract (I do)

`spring-boot-starter-graphql` looks for `src/main/resources/graphql/*.graphqls`. Open
[`schema.graphqls`](../src/main/resources/graphql/schema.graphqls) and read it like a menu:

```graphql
type Query {
    todos(completed: Boolean): [Todo!]!   # list of non-null Todos, and the list itself is never null
    todo(id: ID!): Todo                   # may return null
    me: User!
}

type Mutation {
    createTodo(input: CreateTodoInput!): Todo!
    toggleTodo(id: ID!): Todo!
    deleteTodo(id: ID!): Boolean!
}

input CreateTodoInput { title: String!, description: String, priority: Priority }
enum Priority { LOW MEDIUM HIGH }

type Todo { id: ID!  title: String!  description: String  completed: Boolean!
            priority: Priority!  createdAt: String!  owner: User! }

type User { username: String!  role: String!  todoCount: Int! }
```

> 🔮 **Predict:** `AppUser` has a `password` field. Can a client query `me { password }`?
> No: it's not in the schema, so the query is **rejected before any Java runs**. The schema is a whitelist.

### Step 2: resolvers (We do)

```java
@Controller                                     // plain @Controller, not @RestController
public class TodoGraphQlController {

    private final TodoService todoService;      // the SAME service the REST controller uses
    ...

    @QueryMapping                               // method name "todos" = field Query.todos
    public List<Todo> todos(@Argument Boolean completed, Principal principal) {
        return todoService.findAll(principal.getName(), completed);
    }

    @QueryMapping
    public Todo todo(@Argument Long id, Principal principal) {
        return todoService.findById(id, principal.getName());
    }

    @MutationMapping
    public Todo createTodo(@Argument @Valid CreateTodoRequest input, Principal principal) {
        return todoService.create(principal.getName(), input);   // the GraphQL input binds into our REST DTO!
    }
}
```

We return the **entity** `Todo`, unlike in REST. Is that safe here? Yes: the schema decides which fields go out, so the entity
never gets serialized wholesale. `Todo.owner` resolves automatically because the entity has `getOwner()`.

> 💡 Security still works: `/graphql` falls under `anyRequest().authenticated()`, and `Principal` arrives just like in REST.
> `@PreAuthorize` on `TodoService` would protect GraphQL too. **Session 2 + 4 paying off.**

### Step 3: a field that isn't stored, `@SchemaMapping` (I do)

`User.todoCount` doesn't exist on `AppUser`. Tell Spring how to compute it:

```java
@SchemaMapping(typeName = "User", field = "todoCount")
public long todoCount(AppUser user) {           // the parent object is passed in
    return todoRepository.countByOwnerUsername(user.getUsername());
}
```

It runs **only if a client asks for `todoCount`**. Unrequested fields cost nothing.

### Step 4: errors the GraphQL way

```java
@GraphQlExceptionHandler
public GraphQLError handleNotFound(TodoNotFoundException ex) {
    return GraphQLError.newError().errorType(ErrorType.NOT_FOUND).message(ex.getMessage()).build();
}
```

Without it, clients see `INTERNAL_ERROR`. With it:

```json
{ "errors": [{ "message": "Todo 3 was not found", "extensions": { "classification": "NOT_FOUND" } }],
  "data": { "todo": null } }
```

> 🔮 **Predict:** What HTTP status code comes with that error? **200.** In GraphQL, a request can partly succeed
> (some fields have data, others errors), so errors live in the body. Clients must check `errors`.

### Step 5: play in GraphiQL (everyone, interactive)

Open http://localhost:8080/graphiql. Click **Headers** (bottom panel) and paste:

```json
{ "Authorization": "Basic YWxpY2U6cGFzc3dvcmQxMjM=" }
```

(That's `alice:password123` in Base64, as in Session 4. Your browser might also just pop up a login box.)

Work through these together. Use **Ctrl+Space** for autocomplete, which comes from the schema:

```graphql
# 1. Only titles: no over-fetching
{ todos { title } }

# 2. The dashboard in one request: no under-fetching
{ me { username role todoCount }  open: todos(completed: false) { id title priority } }

# 3. Aliases: same field twice with different arguments
{ done: todos(completed: true) { title }  notDone: todos(completed: false) { title } }

# 4. Mutation
mutation { createTodo(input: { title: "Learn GraphQL", priority: HIGH }) { id title createdAt } }

# 5. Variables: how real clients send arguments (fill the Variables panel with {"id": 1})
query OneTodo($id: ID!) { todo(id: $id) { title completed owner { username } } }

# 6. Fragments: reuse a selection
fragment Basics on Todo { id title completed }
{ todos { ...Basics priority } }

# 7. Errors: admin's todo
{ todo(id: 3) { title } }
```

The same thing with curl:

```bash
curl -u alice:password123 -H 'Content-Type: application/json' \
     -d '{"query":"{ me { username todoCount } todos { title } }"}' localhost:8080/graphql
```

---

## ✅ Checkpoint

- Query #2 returns `me` and `open` in one response.
- `createTodo` with an empty title returns a `BAD_REQUEST` error ("Invalid input: title is required").
- A request without the `Authorization` header gets HTTP **401** (security runs before GraphQL).
- `./mvnw test -Dtest=TodoGraphQlTest` is green.

---

## 💥 Break it on purpose

| Change | What you'll see | Lesson |
|--------|-----------------|--------|
| Query a field not in the schema: `{ todos { secret } }` | `ValidationError: Field 'secret' in type 'Todo' is undefined` | Validated against the schema before any Java runs |
| Rename the Java method `todos` → `allTodos` | Startup log: *schema mapping inspection* lists `Query.todos` as unmapped; queries return `null` / errors | Method names must match schema field names (or use `@QueryMapping("todos")`) |
| Make `Query.todo` non-null (`Todo!`) and request a missing id | Error *declared as a non null type, but … returned a null value*, and `data` becomes `null` | `!` is a promise; breaking it "bubbles" null up to the parent |
| Remove the `@GraphQlExceptionHandler` | `INTERNAL_ERROR for <uuid>` | Unknown exceptions are hidden from clients (good for security, bad for usability) |
| Ask for `{ todos { owner { todoCount } } }` with `show-sql=true` | One `count` query **per todo** | The N+1 problem (see 🔴 exercise) |

---

## 🧪 Exercises

- 🟢 Add `updateTodo(id: ID!, input: UpdateTodoInput!): Todo!` to the schema and a `@MutationMapping` that calls `todoService.update`.
- 🟢 Add `search(text: String!): [Todo!]!` to `Query`, reusing `todoService.search`.
- 🟡 Add `todos: [Todo!]!` to `type User` with a `@SchemaMapping`, so `{ me { username todos { title } } }` works.
- 🟡 Add an admin-only query `allTodos: [Todo!]!` that calls `findAllForAdmin()`. Verify Alice gets a `FORBIDDEN` error without you writing any extra security code.
- 🔴 **Fix N+1** with `@BatchMapping`: one query for all users' counts:
  ```java
  @BatchMapping(typeName = "User", field = "todoCount")
  public Map<AppUser, Long> todoCount(List<AppUser> users) { … one grouped query … }
  ```
- 🔴 Rate-limit `/graphql` (the Session 5 🔴 exercise, if you skipped it).

---

## ❓ Quiz

1. What are over-fetching and under-fetching? Give a Todo example of each.
2. In `todos(completed: Boolean): [Todo!]!`, what do the two `!` mean?
3. What's the difference between `@QueryMapping` and `@SchemaMapping`?
4. A GraphQL request returned HTTP 200. Does that mean it succeeded?
5. Why didn't we have to write new business logic or new security checks for GraphQL?

<details><summary>Answers</summary>

1. Over: getting fields you don't need (mobile gets `description`, `createdAt`…). Under: needing several requests to build one screen (`/me` + `/todos` + count).
2. `Todo!`: items in the list are never null. `]!`: the list itself is never null (may be empty).
3. `@QueryMapping` resolves a top-level field on `Query`; `@SchemaMapping` resolves a field on any type (e.g. `User.todoCount`). `@QueryMapping` is a shortcut for `@SchemaMapping(typeName = "Query")`.
4. Not necessarily: check the `errors` array in the body.
5. The resolvers call the same `TodoService` (DI), and security runs in filters / `@PreAuthorize`, independent of REST vs GraphQL.
</details>

## 🧭 Recap

- The schema is a typed contract and a whitelist; clients pick fields; one endpoint serves all.
- Resolvers are thin: they call the same services as REST. Architecture from Session 2 makes adding GraphQL cheap.
- Errors come back in the body; watch for N+1 and fix it with `@BatchMapping`.

---

## 🎓 Facilitator notes

- GraphiQL is the star of this session. Let learners explore with autocomplete for 10 minutes before explaining anything.
- **"Should we replace REST with GraphQL?"** Usually not. Many teams run both (exactly like this app). GraphQL shines with many clients and nested data; REST is simpler for CRUD, caching and public APIs.
- The `ID` type is serialized as a string (`"id": "7"`) even though it's a `Long` in Java. Learners notice and ask.
- `createdAt` is a `String` in the schema to keep things simple. Custom scalars (`DateTime`) are a good parking-lot topic.
- Subscriptions (real-time over WebSocket) exist; mention them, don't teach them today.

---

## 🏁 After Session 6: capstone

Add **tags** to todos end to end, alone or in pairs:
`Tag` entity (`@ManyToMany`) → repository with a derived query → service method → REST endpoint (validated, rate-limited) →
GraphQL field + mutation → only the owner can tag their todos → tests for each layer. Present it in 5 minutes.
