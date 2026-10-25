package org.todo.spring_todo.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.todo.spring_todo.todo.Priority;

/**
 * SESSION 2 — A DTO (Data Transfer Object): the exact shape of JSON a client may send.
 * Validation annotations are checked when the controller parameter is marked @Valid.
 */
public record CreateTodoRequest(
        @NotBlank(message = "title is required")
        @Size(max = 100, message = "title must be at most 100 characters")
        String title,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description,

        Priority priority
) {
}
