package org.todo.spring_todo.todo;

import jakarta.validation.Valid;
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
    public Todo create(@Valid @RequestBody CreateTodoRequest request) {
        long id = nextId.getAndIncrement();
        Todo todo = new Todo(id, request.title(), false);
        todos.put(id, todo);
        return todo;
    }

    @DeleteMapping("/{id}")                               // DELETE /api/todos/7
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return todos.remove(id) != null
                ? ResponseEntity.noContent().build()      // 204
                : ResponseEntity.notFound().build();      // 404
    }
}
