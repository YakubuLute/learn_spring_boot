package org.todo.spring_todo.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.todo.spring_todo.ratelimit.RateLimited;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Public (see SecurityConfig). Tightly rate-limited so bots can't mass-create accounts.
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimited(requests = 3, perSeconds = 60)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(userService.register(request));
    }

    // "Who am I?" — handy to test that your credentials work.
    @GetMapping("/me")
    public UserResponse me(Principal principal) {
        return UserResponse.from(userService.getByUsername(principal.getName()));
    }
}
