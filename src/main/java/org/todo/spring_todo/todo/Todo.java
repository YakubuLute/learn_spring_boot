package org.todo.spring_todo.todo;

import java.time.Instant;

public record Todo(Long id, String title, boolean completed, Instant createdAt) {
}
