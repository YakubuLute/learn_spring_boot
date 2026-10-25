package org.todo.spring_todo.todo.dto;

import org.todo.spring_todo.todo.Priority;
import org.todo.spring_todo.todo.Todo;

import java.time.Instant;

/**
 * What we send back. Never return the entity directly: it ties your API shape to your
 * database shape (and in Session 4 it would leak password hashes).
 */
public record TodoResponse(
        Long id,
        String title,
        String description,
        boolean completed,
        Priority priority,
        Instant createdAt
) {
    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.isCompleted(),
                todo.getPriority(),
                todo.getCreatedAt());
    }
}
