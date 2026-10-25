package org.todo.spring_todo.graphql;

import graphql.GraphQLError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;
import org.todo.spring_todo.todo.Todo;
import org.todo.spring_todo.todo.TodoNotFoundException;
import org.todo.spring_todo.todo.TodoRepository;
import org.todo.spring_todo.todo.TodoService;
import org.todo.spring_todo.todo.dto.CreateTodoRequest;
import org.todo.spring_todo.user.AppUser;
import org.todo.spring_todo.user.UserService;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SESSION 6 — GraphQL "resolvers". Each method answers one field of the schema.
 * Notice we REUSE TodoService: the business logic doesn't care whether
 * the request came in via REST or GraphQL. That's the payoff of Session 2.
 */
@Controller   // plain @Controller, not @RestController
public class TodoGraphQlController {

    private final TodoService todoService;
    private final UserService userService;
    private final TodoRepository todoRepository;

    public TodoGraphQlController(TodoService todoService, UserService userService, TodoRepository todoRepository) {
        this.todoService = todoService;
        this.userService = userService;
        this.todoRepository = todoRepository;
    }

    // type Query { todos(completed: Boolean): [Todo!]! }
    @QueryMapping
    public List<Todo> todos(@Argument Boolean completed, Principal principal) {
        return todoService.findAll(principal.getName(), completed);
    }

    // type Query { todo(id: ID!): Todo }
    @QueryMapping
    public Todo todo(@Argument Long id, Principal principal) {
        return todoService.findById(id, principal.getName());
    }

    @QueryMapping
    public AppUser me(Principal principal) {
        return userService.getByUsername(principal.getName());
    }

    // type Mutation { createTodo(input: CreateTodoInput!): Todo! }
    // The "input" object is bound straight into our existing CreateTodoRequest record.
    @MutationMapping
    public Todo createTodo(@Argument @Valid CreateTodoRequest input, Principal principal) {
        return todoService.create(principal.getName(), input);
    }

    @MutationMapping
    public Todo toggleTodo(@Argument Long id, Principal principal) {
        return todoService.toggle(id, principal.getName());
    }

    @MutationMapping
    public boolean deleteTodo(@Argument Long id, Principal principal) {
        todoService.delete(id, principal.getName());
        return true;
    }

    // type User { todoCount: Int! }  — a field that isn't a Java property.
    // Spring calls this once per User object in the response.
    @SchemaMapping(typeName = "User", field = "todoCount")
    public long todoCount(AppUser user) {
        return todoRepository.countByOwnerUsername(user.getUsername());
    }

    // Turn our exception into a tidy GraphQL error instead of "INTERNAL_ERROR".
    @GraphQlExceptionHandler
    public GraphQLError handleNotFound(TodoNotFoundException ex) {
        return GraphQLError.newError()
                .errorType(ErrorType.NOT_FOUND)
                .message(ex.getMessage())
                .build();
    }

    // @Valid on a GraphQL @Argument throws ConstraintViolationException when a rule is broken.
    @GraphQlExceptionHandler
    public GraphQLError handleInvalidInput(ConstraintViolationException ex) {
        String details = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining(", "));
        return GraphQLError.newError()
                .errorType(ErrorType.BAD_REQUEST)
                .message("Invalid input: " + details)
                .build();
    }
}
