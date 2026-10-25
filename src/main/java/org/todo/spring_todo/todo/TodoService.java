package org.todo.spring_todo.todo;

import org.springframework.stereotype.Service;
import org.todo.spring_todo.notification.Notifier;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * SESSION 2 — The business logic lives here, not in the controller.
 * Every dependency arrives through the constructor (constructor injection).
 */
@Service
public class TodoService {

    private final TodoRepository todoRepository;
    private final Notifier notifier;
    private final Clock clock;

    // One constructor => Spring uses it automatically. No @Autowired needed.
    public TodoService(TodoRepository todoRepository, Notifier notifier, Clock clock) {
        this.todoRepository = todoRepository;
        this.notifier = notifier;
        this.clock = clock;
    }

    public List<Todo> findAll() {
        return todoRepository.findAll();
    }

    public Todo findById(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    public Todo create(CreateTodoRequest request) {
        Todo saved = todoRepository.save(new Todo(null, request.title(), false, Instant.now(clock)));
        notifier.notify("someone", "Created todo: " + saved.title());   // real username in Session 4
        return saved;
    }

    public void delete(Long id) {
        findById(id);                    // throws 404 if missing
        todoRepository.deleteById(id);
    }
}
