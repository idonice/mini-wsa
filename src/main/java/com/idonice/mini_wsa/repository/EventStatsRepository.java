package com.idonice.mini_wsa.repository;

import com.idonice.mini_wsa.dto.stats.TopAttacker;
import com.idonice.mini_wsa.dto.stats.TopPath;
import com.idonice.mini_wsa.model.Event;
import com.idonice.mini_wsa.repository.projection.CategoryActionCount;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;

// Read-only aggregation queries for GET /v1/stats/summary.
// Every query: optional configId (null = all configs), time range [from, to).
public interface EventStatsRepository extends Repository<Event, String> {

    @Query("""
            SELECT new com.idonice.mini_wsa.repository.projection.CategoryActionCount(
                       e.rule.category, e.action, COUNT(e), SUM(e.threatScore))
            FROM Event e
            WHERE (:configId IS NULL OR e.configId = :configId)
              AND e.timestamp >= :from AND e.timestamp < :to
            GROUP BY e.rule.category, e.action
            """)
    List<CategoryActionCount> countByCategoryAndAction(Long configId, Instant from, Instant to);

    @Query("""
            SELECT new com.idonice.mini_wsa.dto.stats.TopAttacker(
                       e.clientIp, COUNT(e), AVG(e.threatScore))
            FROM Event e
            WHERE (:configId IS NULL OR e.configId = :configId)
              AND e.timestamp >= :from AND e.timestamp < :to
            GROUP BY e.clientIp
            ORDER BY COUNT(e) DESC, e.clientIp
            """)
    List<TopAttacker> findTopAttackers(Long configId, Instant from, Instant to, Limit limit);

    @Query("""
            SELECT new com.idonice.mini_wsa.dto.stats.TopPath(e.path, COUNT(e))
            FROM Event e
            WHERE (:configId IS NULL OR e.configId = :configId)
              AND e.timestamp >= :from AND e.timestamp < :to
            GROUP BY e.path
            ORDER BY COUNT(e) DESC, e.path
            """)
    List<TopPath> findTopPaths(Long configId, Instant from, Instant to, Limit limit);
}
