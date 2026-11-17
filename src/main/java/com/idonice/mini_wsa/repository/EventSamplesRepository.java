package com.idonice.mini_wsa.repository;

import com.idonice.mini_wsa.model.Action;
import com.idonice.mini_wsa.model.Category;
import com.idonice.mini_wsa.model.Event;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;

// Read-only queries for GET /v1/events/samples.
// configId, category, action: null = not filtered. from/to are always set (the service fills defaults).
public interface EventSamplesRepository extends Repository<Event, String> {

    @Query("""
            SELECT e FROM Event e
            WHERE (:configId IS NULL OR e.configId = :configId)
              AND e.timestamp >= :from AND e.timestamp < :to
              AND (:category IS NULL OR e.rule.category = :category)
              AND (:action IS NULL OR e.action = :action)
            ORDER BY e.timestamp DESC, e.eventId DESC
            LIMIT :limit OFFSET :offset
            """)
    List<Event> findPage(Long configId, Instant from, Instant to,
                         Category category, Action action, int limit, int offset);

    @Query("""
            SELECT COUNT(e) FROM Event e
            WHERE (:configId IS NULL OR e.configId = :configId)
              AND e.timestamp >= :from AND e.timestamp < :to
              AND (:category IS NULL OR e.rule.category = :category)
              AND (:action IS NULL OR e.action = :action)
            """)
    long count(Long configId, Instant from, Instant to, Category category, Action action);
}
