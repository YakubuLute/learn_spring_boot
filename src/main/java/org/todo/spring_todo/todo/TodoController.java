package org.todo.spring_todo.todo;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.todo.spring_todo.ratelimit.RateLimited;
import org.todo.spring_todo.todo.dto.CreateTodoRequest;
import org.todo.spring_todo.todo.dto.TodoResponse;
import org.todo.spring_todo.todo.dto.UpdateTodoRequest;

import java.net.URI;
import java.security.Principal;
import java.util.List;

/**
 * SESSION 1 — The web layer: turns HTTP requests into Java method calls and back.
 * It stays thin: read the request, call the service, shape the response.
 */
@RestController                 // = @Controller + @ResponseBody: return values become JSON
@RequestMapping("/api/todos")   // every route below starts with /api/todos
@RateLimited(requests = 30, perSeconds = 60)   // SESSION 5: default for this controller
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    // GET /api/todos            -> all my todos
    // GET /api/todos?completed=true -> only finished ones
    @GetMapping
    public List<TodoResponse> list(@RequestParam(required = false) Boolean completed,
                                   Principal principal) {   // SESSION 4: who is calling?
        return todoService.findAll(principal.getName(), completed).stream()
                .map(TodoResponse::from)
                .toList();
    }

    // GET /api/todos/search?q=milk
    @GetMapping("/search")
    public List<TodoResponse> search(@RequestParam("q") String text, Principal principal) {
        return todoService.search(principal.getName(), text).stream()
                .map(TodoResponse::from)
                .toList();
    }

    // GET /api/todos/42
    @GetMapping("/{id}")
    public TodoResponse get(@PathVariable Long id, Principal principal) {
        return TodoResponse.from(todoService.findById(id, principal.getName()));
    }

    // POST /api/todos  {"title": "Buy milk"}
    @PostMapping
    @RateLimited(requests = 5, perSeconds = 60)   // method-level beats class-level
    public ResponseEntity<TodoResponse> create(@Valid @RequestBody CreateTodoRequest request,
                                               Principal principal) {
        Todo saved = todoService.create(principal.getName(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(TodoResponse.from(saved));   // 201 + Location header
    }

    // PUT /api/todos/42  (replace all editable fields)
    @PutMapping("/{id}")
    public TodoResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateTodoRequest request,
                               Principal principal) {
        return TodoResponse.from(todoService.update(id, principal.getName(), request));
    }

    // PATCH /api/todos/42/toggle
    @PatchMapping("/{id}/toggle")
    public TodoResponse toggle(@PathVariable Long id, Principal principal) {
        return TodoResponse.from(todoService.toggle(id, principal.getName()));
    }

    // DELETE /api/todos/42
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)   // 204: success, nothing to send back
    public void delete(@PathVariable Long id, Principal principal) {
        todoService.delete(id, principal.getName());
    }
}
