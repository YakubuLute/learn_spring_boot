package org.todo.spring_todo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.todo.spring_todo.todo.Priority;
import org.todo.spring_todo.todo.Todo;
import org.todo.spring_todo.todo.TodoRepository;
import org.todo.spring_todo.user.AppUser;
import org.todo.spring_todo.user.AppUserRepository;
import org.todo.spring_todo.user.Role;

import java.time.Clock;
import java.time.Instant;

/**
 * Demo data so the API isn't empty on first run. A CommandLineRunner bean runs once,
 * right after the application context has started.
 * DEV/TRAINING ONLY — never ship hard-coded passwords.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppUserRepository userRepository;
    private final TodoRepository todoRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DataSeeder(AppUserRepository userRepository, TodoRepository todoRepository,
                      PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.todoRepository = todoRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;   // already seeded (e.g. a persistent PostgreSQL database)
        }
        AppUser alice = userRepository.save(
                new AppUser("alice", passwordEncoder.encode("password123"), Role.USER));
        AppUser admin = userRepository.save(
                new AppUser("admin", passwordEncoder.encode("admin12345"), Role.ADMIN));

        Instant now = Instant.now(clock);
        todoRepository.save(new Todo("Learn dependency injection", "Session 2", Priority.HIGH, alice, now));
        todoRepository.save(new Todo("Buy milk", null, Priority.LOW, alice, now.plusSeconds(1)));
        todoRepository.save(new Todo("Review learner labs", "Admin only sees this", Priority.MEDIUM, admin, now));

        log.info("Seeded demo users: alice/password123 (USER), admin/admin12345 (ADMIN)");
    }
}
