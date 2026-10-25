package org.todo.spring_todo.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A pretend email sender. To use it instead of LogNotifier, inject with
 * {@code @Qualifier("emailNotifier") Notifier notifier}. (Exercise in Session 2.)
 */
@Component("emailNotifier")   // bean name; the default would be "emailNotifier" anyway
public class EmailNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    @Override
    public void notify(String username, String message) {
        log.info("📧 (pretend) email to {}: {}", username, message);
    }
}
