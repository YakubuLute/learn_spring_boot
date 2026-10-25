package org.todo.spring_todo.graphql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SESSION 6 — Send real GraphQL documents through the full stack, security included.
 * RANDOM_PORT starts a real server, so requests travel over HTTP exactly like curl or GraphiQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
class TodoGraphQlTest {

    @Autowired
    HttpGraphQlTester graphQlTester;

    private HttpGraphQlTester asAlice() {
        return graphQlTester.mutate()
                .headers(headers -> headers.setBasicAuth("alice", "password123"))
                .build();
    }

    @Test
    void meReturnsTheLoggedInUser() {
        asAlice().document("{ me { username role } }")
                .execute()
                .path("me.username").entity(String.class).isEqualTo("alice")
                .path("me.role").entity(String.class).isEqualTo("USER");
    }

    @Test
    void createTodoMutation() {
        String title = asAlice().document("""
                        mutation {
                          createTodo(input: { title: "From GraphQL", priority: HIGH }) {
                            title priority completed owner { username }
                          }
                        }
                        """)
                .execute()
                .path("createTodo.priority").entity(String.class).isEqualTo("HIGH")
                .path("createTodo.owner.username").entity(String.class).isEqualTo("alice")
                .path("createTodo.title").entity(String.class).get();

        assertThat(title).isEqualTo("From GraphQL");
    }

    @Test
    void someoneElsesTodoIsNotFound() {
        // Todo 3 belongs to admin (see DataSeeder)
        asAlice().document("{ todo(id: 3) { title } }")
                .execute()
                .errors()
                .expect(error -> error.getErrorType() == ErrorType.NOT_FOUND);
    }
}
