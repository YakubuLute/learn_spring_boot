package org.todo.spring_todo.todo;

public class TodoNotFoundException extends RuntimeException {

    public TodoNotFoundException(Long id) {
        super("Todo " + id + " was not found");
    }
}
