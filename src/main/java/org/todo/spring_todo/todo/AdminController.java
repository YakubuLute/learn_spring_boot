package org.todo.spring_todo.todo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.todo.spring_todo.todo.dto.TodoResponse;

import java.util.List;

/**
 * SESSION 4 — Protected twice: the URL rule in SecurityConfig (/api/admin/** needs ADMIN)
 * and @PreAuthorize on TodoService.findAllForAdmin(). Defence in depth.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final TodoService todoService;

    public AdminController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/todos")
    public List<TodoResponse> allTodos() {
        return todoService.findAllForAdmin().stream()
                .map(TodoResponse::from)
                .toList();
    }
}
