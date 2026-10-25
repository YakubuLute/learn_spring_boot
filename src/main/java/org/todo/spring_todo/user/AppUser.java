package org.todo.spring_todo.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SESSION 4 — Our own user table. Called AppUser / app_user because "user" is a
 * reserved word in H2 and PostgreSQL.
 */
@Entity
@Table(name = "app_user")
@Getter @Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false)
    private String password;   // a BCrypt HASH, never the real password

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    public AppUser(String username, String passwordHash, Role role) {
        this.username = username;
        this.password = passwordHash;
        this.role = role;
    }
}
