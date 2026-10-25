package org.todo.spring_todo.todo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * SESSION 3 — We only write an interface. Spring Data generates the implementation at startup.
 * findAll(), findById(), save(), deleteById(), count() ... come for free from JpaRepository.
 */
public interface TodoRepository extends JpaRepository<Todo, Long> {

    // "Derived queries": Spring reads the method name and writes the SQL for you.
    List<Todo> findAllByOrderByCreatedAtDesc();

    List<Todo> findByCompletedOrderByCreatedAtDesc(boolean completed);

    List<Todo> findByTitleContainingIgnoreCase(String text);

    long countByCompleted(boolean completed);

    // When the method name would get silly, write JPQL yourself.
    // JPQL talks about classes and fields (Todo, t.title), not tables and columns.
    @Query("""
            select t from Todo t
            where lower(t.title) like lower(concat('%', :text, '%'))
            order by t.createdAt desc
            """)
    List<Todo> searchByTitle(@Param("text") String text);
}
