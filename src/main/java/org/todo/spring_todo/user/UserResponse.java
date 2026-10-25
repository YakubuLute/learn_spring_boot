package org.todo.spring_todo.user;

/** Notice: no password field. What you leave out of a DTO can never leak. */
public record UserResponse(Long id, String username, Role role) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
