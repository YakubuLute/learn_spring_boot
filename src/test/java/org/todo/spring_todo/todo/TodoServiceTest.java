package org.todo.spring_todo.todo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.todo.spring_todo.todo.dto.CreateTodoRequest;
import org.todo.spring_todo.user.AppUser;
import org.todo.spring_todo.user.Role;
import org.todo.spring_todo.user.UserService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SESSION 2 — The payoff of constructor injection: a plain unit test.
 * No Spring, no database, no web server. We hand TodoService fakes and a frozen clock.
 */
class TodoServiceTest {

    private final Instant fixedNow = Instant.parse("2026-01-01T09:00:00Z");
    private final List<String> notifications = new ArrayList<>();

    private TodoRepository todoRepository;
    private UserService userService;
    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoRepository = mock(TodoRepository.class);
        userService = mock(UserService.class);
        Clock frozenClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

        // Notifier has one method, so a lambda is a perfectly good fake implementation.
        todoService = new TodoService(todoRepository, userService,
                (username, message) -> notifications.add(username + ": " + message),
                frozenClock);
    }

    @Test
    void createSetsOwnerTimestampAndDefaultPriority() {
        AppUser alice = new AppUser("alice", "hash", Role.USER);
        when(userService.getByUsername("alice")).thenReturn(alice);
        when(todoRepository.save(any(Todo.class))).thenAnswer(call -> call.getArgument(0));

        Todo created = todoService.create("alice", new CreateTodoRequest("Buy milk", null, null));

        assertThat(created.getOwner()).isEqualTo(alice);
        assertThat(created.getCreatedAt()).isEqualTo(fixedNow);
        assertThat(created.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(notifications).containsExactly("alice: Created todo: Buy milk");
    }

    @Test
    void findByIdThrowsWhenTodoBelongsToSomeoneElse() {
        when(todoRepository.findByIdAndOwnerUsername(1L, "bob")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.findById(1L, "bob"))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("Todo 1 was not found");
    }

    @Test
    void toggleFlipsCompletedAndNotifiesOnlyWhenCompleting() {
        Todo todo = new Todo("Read docs", null, Priority.LOW, new AppUser("alice", "hash", Role.USER), fixedNow);
        when(todoRepository.findByIdAndOwnerUsername(7L, "alice")).thenReturn(Optional.of(todo));

        todoService.toggle(7L, "alice");
        assertThat(todo.isCompleted()).isTrue();

        todoService.toggle(7L, "alice");
        assertThat(todo.isCompleted()).isFalse();

        assertThat(notifications).containsExactly("alice: Completed: Read docs");
        verify(todoRepository, never()).save(any());   // dirty checking, remember?
    }
}
