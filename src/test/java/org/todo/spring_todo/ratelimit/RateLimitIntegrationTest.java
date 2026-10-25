package org.todo.spring_todo.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void sixthCreateWithinAMinuteGets429() throws Exception {
        // POST /api/todos is @RateLimited(requests = 5, perSeconds = 60).
        // We use "admin" because no other test creates todos as admin.
        for (int i = 1; i <= 5; i++) {
            mvc.perform(createTodo("Todo " + i)).andExpect(status().isCreated());
        }

        mvc.perform(createTodo("One too many"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
    }

    private MockHttpServletRequestBuilder createTodo(String title) {
        return post("/api/todos")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"" + title + "\"}");
    }
}
