package org.todo.spring_todo.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Two beans implement Notifier. @Primary says: "if someone asks for a Notifier
 * and doesn't say which one, give them me".
 */
@Component
@Primary
public class LogNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(LogNotifier.class);

    @Override
    public void notify(String username, String message) {
        log.info("[notify {}] {}", username, message);
    }
}
