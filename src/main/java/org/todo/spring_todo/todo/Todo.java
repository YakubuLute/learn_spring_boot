package org.todo.spring_todo.todo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * SESSION 3 — A JPA entity: one instance of this class == one row in the "todo" table.
 */
@Entity                         // "Hibernate, please map this class to a table"
@Table(name = "todo")
@Getter @Setter                 // Lombok writes getters/setters for us at compile time
@NoArgsConstructor              // JPA needs a no-args constructor to create objects from rows
public class Todo {

    @Id                                                   // primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // the database picks the number
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    private boolean completed;

    @Enumerated(EnumType.STRING)  // store "HIGH" instead of 2 — survives re-ordering the enum
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Todo(String title, String description, Priority priority, Instant createdAt) {
        this.title = title;
        this.description = description;
        this.priority = priority == null ? Priority.MEDIUM : priority;
        this.createdAt = createdAt;
    }
}
