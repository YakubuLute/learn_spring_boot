package org.todo.spring_todo.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SESSION 4 — The whole application starts (with the seeded users), and MockMvc sends
 * fake HTTP requests through the real security filter chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TodoApiSecurityTest {

    @Autowired
    MockMvc mvc;

    @Test
    void anonymousCallerGets401() throws Exception {
        mvc.perform(get("/api/todos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordGets401() throws Exception {
        mvc.perform(get("/api/todos").with(httpBasic("alice", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aliceSeesOnlyHerOwnTodos() throws Exception {
        mvc.perform(get("/api/todos").with(httpBasic("alice", "password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].owner").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("alice"))));
    }

    @Test
    void userCannotReachAdminEndpoints() throws Exception {
        mvc.perform(get("/api/admin/todos").with(httpBasic("alice", "password123")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/todos").with(httpBasic("admin", "admin12345")))
                .andExpect(status().isOk());
    }

    @Test
    void invalidBodyGets400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/todos").with(httpBasic("alice", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("title is required"));
    }
}
