package org.todo.spring_todo.notification;

/**
 * SESSION 2 — Depend on an interface, not a concrete class.
 * TodoService only knows "something that can notify". Spring decides which one.
 */
public interface Notifier {

    void notify(String username, String message);
}
