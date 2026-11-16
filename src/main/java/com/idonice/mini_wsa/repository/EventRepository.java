package com.idonice.mini_wsa.repository;

import com.idonice.mini_wsa.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface EventRepository extends JpaRepository<Event, String> {

    // Repeat-offender check. "Between" includes both ends. Uses the (client_ip, timestamp) index.
    long countByClientIpAndTimestampBetween(String clientIp, Instant from, Instant to);
}
