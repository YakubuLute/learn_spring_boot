package org.todo.spring_todo.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.todo.spring_todo.todo.Priority;

public record UpdateTodoRequest(
        @NotBlank(message = "title is required")
        @Size(max = 100, message = "title must be at most 100 characters")
        String title,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description,

        @NotNull(message = "priority is required")
        Priority priority,

        boolean completed
) {
}
