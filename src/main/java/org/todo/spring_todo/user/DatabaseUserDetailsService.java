package org.todo.spring_todo.user;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * SESSION 4 — The bridge between OUR user table and Spring Security.
 * On every request with credentials, Spring Security calls loadUserByUsername(),
 * then checks the submitted password against the stored hash itself.
 * Because this bean exists, Spring Boot stops generating the random "user" password.
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public DatabaseUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user " + username));
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())   // becomes the authority "ROLE_USER" / "ROLE_ADMIN"
                .build();
    }
}
