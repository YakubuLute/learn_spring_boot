package org.todo.spring_todo.todo;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.todo.spring_todo.todo.dto.CreateTodoRequest;
import org.todo.spring_todo.todo.dto.TodoResponse;
import org.todo.spring_todo.todo.dto.UpdateTodoRequest;

import java.net.URI;
import java.util.List;

/**
 * SESSION 1 — The web layer: turns HTTP requests into Java method calls and back.
 * It stays thin: read the request, call the service, shape the response.
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    // GET /api/todos               -> all todos
    // GET /api/todos?completed=true -> only finished ones
    @GetMapping
    public List<TodoResponse> list(@RequestParam(required = false) Boolean completed) {
        return todoService.findAll(completed).stream()
                .map(TodoResponse::from)
                .toList();
    }

    // GET /api/todos/search?q=milk
    @GetMapping("/search")
    public List<TodoResponse> search(@RequestParam("q") String text) {
        return todoService.search(text).stream()
                .map(TodoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TodoResponse get(@PathVariable Long id) {
        return TodoResponse.from(todoService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TodoResponse> create(@Valid @RequestBody CreateTodoRequest request) {
        Todo saved = todoService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(TodoResponse.from(saved));   // 201 + Location header
    }

    @PutMapping("/{id}")
    public TodoResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTodoRequest request) {
        return TodoResponse.from(todoService.update(id, request));
    }

    @PatchMapping("/{id}/toggle")
    public TodoResponse toggle(@PathVariable Long id) {
        return TodoResponse.from(todoService.toggle(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        todoService.delete(id);
    }
}
