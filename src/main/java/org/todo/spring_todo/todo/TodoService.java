package org.todo.spring_todo.todo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.todo.spring_todo.notification.Notifier;
import org.todo.spring_todo.todo.dto.CreateTodoRequest;
import org.todo.spring_todo.todo.dto.UpdateTodoRequest;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * SESSION 2 — The business logic lives here, not in the controller.
 * SESSION 3 — Same service, now backed by a real database through TodoRepository.
 */
@Service
@Transactional  // SESSION 3: every public method runs inside a database transaction
public class TodoService {

    private final TodoRepository todoRepository;
    private final Notifier notifier;
    private final Clock clock;

    public TodoService(TodoRepository todoRepository, Notifier notifier, Clock clock) {
        this.todoRepository = todoRepository;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Todo> findAll(Boolean completed) {
        if (completed == null) {
            return todoRepository.findAllByOrderByCreatedAtDesc();
        }
        return todoRepository.findByCompletedOrderByCreatedAtDesc(completed);
    }

    @Transactional(readOnly = true)
    public Todo findById(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Todo> search(String text) {
        return todoRepository.searchByTitle(text);
    }

    public Todo create(CreateTodoRequest request) {
        Todo saved = todoRepository.save(
                new Todo(request.title(), request.description(), request.priority(), Instant.now(clock)));
        notifier.notify("someone", "Created todo: " + saved.getTitle());   // real username in Session 4
        return saved;
    }

    public Todo update(Long id, UpdateTodoRequest request) {
        Todo todo = findById(id);
        todo.setTitle(request.title());
        todo.setDescription(request.description());
        todo.setPriority(request.priority());
        todo.setCompleted(request.completed());
        // No save() call needed! Inside a transaction, Hibernate notices the changes
        // ("dirty checking") and runs an UPDATE when the method returns.
        return todo;
    }

    public Todo toggle(Long id) {
        Todo todo = findById(id);
        todo.setCompleted(!todo.isCompleted());
        if (todo.isCompleted()) {
            notifier.notify("someone", "Completed: " + todo.getTitle());
        }
        return todo;
    }

    public void delete(Long id) {
        todoRepository.delete(findById(id));
    }
}
