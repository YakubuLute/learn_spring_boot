package org.todo.spring_todo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** SESSION 1 — The smallest possible endpoint. */
@RestController                       // "I handle HTTP and my return values ARE the response body"
public class HelloController {

    @GetMapping("/hello")             // GET /hello  ->  this method
    public String hello(@RequestParam(defaultValue = "Spring") String name) {
        return "Hello, " + name + "!";
    }
}
