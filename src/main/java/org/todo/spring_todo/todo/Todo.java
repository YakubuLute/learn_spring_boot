package org.todo.spring_todo.todo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.todo.spring_todo.user.AppUser;

import java.time.Instant;

/**
 * SESSION 3 — A JPA entity: one instance of this class == one row in the "todo" table.
 * SESSION 4 — Added the {@code owner} relationship so every todo belongs to a user.
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

    @ManyToOne(optional = false)  // many todos -> one user (adds an owner_id foreign key column)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    public Todo(String title, String description, Priority priority, AppUser owner, Instant createdAt) {
        this.title = title;
        this.description = description;
        this.priority = priority == null ? Priority.MEDIUM : priority;
        this.owner = owner;
        this.createdAt = createdAt;
    }
}
