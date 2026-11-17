package com.idonice.mini_wsa.service;

import com.idonice.mini_wsa.dto.event.EnrichedEvent;
import com.idonice.mini_wsa.dto.samples.SamplesResult;
import com.idonice.mini_wsa.model.Action;
import com.idonice.mini_wsa.model.Category;
import com.idonice.mini_wsa.repository.EventSamplesRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class EventSamplesService {

    // Used when from/to are missing. Postgres can't type a null time parameter,
    // and a real range lets the query use the timestamp index.
    private static final Instant EARLIEST = Instant.EPOCH;
    private static final Instant LATEST = Instant.parse("9999-12-31T23:59:59Z");

    private final EventSamplesRepository samplesRepository;

    public EventSamplesService(EventSamplesRepository samplesRepository) {
        this.samplesRepository = samplesRepository;
    }

    public SamplesResult getSamples(Long configId, Instant from, Instant to,
                                    Category category, Action action, int limit, int offset) {
        from = (from != null) ? from : EARLIEST;
        to = (to != null) ? to : LATEST;

        long total = samplesRepository.count(configId, from, to, category, action);

        List<EnrichedEvent> events = samplesRepository
                .findPage(configId, from, to, category, action, limit, offset).stream()
                .map(EnrichedEvent::from)
                .toList();

        return new SamplesResult(total, limit, offset, events);
    }
}
