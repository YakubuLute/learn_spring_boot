package org.todo.spring_todo.todo;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SESSION 2 — Constructor injection pays off: no Spring, just `new` with fakes. */
class TodoServiceTest {

    private final Instant fixedNow = Instant.parse("2026-01-01T09:00:00Z");
    private final List<String> sent = new ArrayList<>();

    private final TodoService service = new TodoService(
            new TodoRepository(),                          // the real in-memory one
            (user, message) -> sent.add(message),          // a lambda IS a Notifier!
            Clock.fixed(fixedNow, ZoneOffset.UTC));

    @Test
    void createUsesTheClockAndNotifies() {
        Todo todo = service.create(new CreateTodoRequest("Buy milk"));

        assertThat(todo.id()).isEqualTo(1L);
        assertThat(todo.createdAt()).isEqualTo(fixedNow);
        assertThat(sent).containsExactly("Created todo: Buy milk");
    }

    @Test
    void findByIdThrowsForUnknownId() {
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("Todo 99 was not found");
    }
}
