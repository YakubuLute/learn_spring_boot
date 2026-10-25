package org.todo.spring_todo.todo;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.todo.spring_todo.notification.Notifier;
import org.todo.spring_todo.todo.dto.CreateTodoRequest;
import org.todo.spring_todo.todo.dto.UpdateTodoRequest;
import org.todo.spring_todo.user.AppUser;
import org.todo.spring_todo.user.UserService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * SESSION 2 — The business logic lives here, not in the controller.
 * Every dependency arrives through the constructor (constructor injection).
 * We never write "new TodoRepository()" — Spring builds and hands us the objects.
 */
@Service
@Transactional  // SESSION 3: every public method runs inside a database transaction
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserService userService;
    private final Notifier notifier;
    private final Clock clock;

    // One constructor => Spring uses it automatically. No @Autowired needed.
    public TodoService(TodoRepository todoRepository,
                       UserService userService,
                       Notifier notifier,
                       Clock clock) {
        this.todoRepository = todoRepository;
        this.userService = userService;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Todo> findAll(String username, Boolean completed) {
        if (completed == null) {
            return todoRepository.findByOwnerUsernameOrderByCreatedAtDesc(username);
        }
        return todoRepository.findByOwnerUsernameAndCompletedOrderByCreatedAtDesc(username, completed);
    }

    @Transactional(readOnly = true)
    public Todo findById(Long id, String username) {
        // Looking up by id AND owner means Bob gets 404 for Alice's todo — he can't even
        // learn that it exists.
        return todoRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Todo> search(String username, String text) {
        return todoRepository.searchByTitle(username, text);
    }

    public Todo create(String username, CreateTodoRequest request) {
        AppUser owner = userService.getByUsername(username);
        Todo todo = new Todo(request.title(), request.description(), request.priority(),
                owner, Instant.now(clock));
        Todo saved = todoRepository.save(todo);
        notifier.notify(username, "Created todo: " + saved.getTitle());
        return saved;
    }

    public Todo update(Long id, String username, UpdateTodoRequest request) {
        Todo todo = findById(id, username);
        todo.setTitle(request.title());
        todo.setDescription(request.description());
        todo.setPriority(request.priority());
        todo.setCompleted(request.completed());
        // No save() call needed! Inside a transaction, Hibernate notices the changes
        // ("dirty checking") and runs an UPDATE when the method returns.
        return todo;
    }

    public Todo toggle(Long id, String username) {
        Todo todo = findById(id, username);
        todo.setCompleted(!todo.isCompleted());
        if (todo.isCompleted()) {
            notifier.notify(username, "Completed: " + todo.getTitle());
        }
        return todo;
    }

    public void delete(Long id, String username) {
        Todo todo = findById(id, username);
        todoRepository.delete(todo);
    }

    // SESSION 4 — method security: checked before the method runs, wherever it's called from.
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Todo> findAllForAdmin() {
        return todoRepository.findAll();
    }
}
