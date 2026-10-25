package org.todo.spring_todo.todo;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SESSION 2 — Storage only. The method names deliberately match Spring Data JPA
 * (findAll, findById, save, deleteById), so Session 3 can swap this for a database.
 */
@Repository
public class TodoRepository {

    private final Map<Long, Todo> store = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public List<Todo> findAll() {
        return List.copyOf(store.values());
    }

    public Optional<Todo> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Todo save(Todo todo) {
        Todo toStore = todo.id() == null
                ? new Todo(nextId.getAndIncrement(), todo.title(), todo.completed(), todo.createdAt())
                : todo;
        store.put(toStore.id(), toStore);
        return toStore;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }
}
