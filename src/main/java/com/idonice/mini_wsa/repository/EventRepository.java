package com.idonice.mini_wsa.repository;

import com.idonice.mini_wsa.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, String> {
}
