package com.idonice.mini_wsa.service;

import com.idonice.mini_wsa.dto.EventError;
import com.idonice.mini_wsa.dto.EventRequest;
import com.idonice.mini_wsa.dto.GeoLocationRequest;
import com.idonice.mini_wsa.dto.IngestResult;
import com.idonice.mini_wsa.dto.RuleRequest;
import com.idonice.mini_wsa.model.Category;
import com.idonice.mini_wsa.model.Event;
import com.idonice.mini_wsa.model.GeoLocation;
import com.idonice.mini_wsa.model.Rule;
import com.idonice.mini_wsa.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Per event: validate -> skip duplicates -> convert -> enrich -> save.
// No @Transactional: each save is its own transaction, so one failure can't roll back the others.
@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final EventValidator eventValidator;
    private final AttackClassifier attackClassifier;
    private final EventRepository eventRepository;

    public IngestService(EventValidator eventValidator,
                         AttackClassifier attackClassifier,
                         EventRepository eventRepository) {
        this.eventValidator = eventValidator;
        this.attackClassifier = attackClassifier;
        this.eventRepository = eventRepository;
    }

    public IngestResult ingest(List<JsonNode> items) {
        Instant receivedAt = Instant.now();
        Set<String> seenIds = new HashSet<>();
        int ingested = 0;
        int duplicates = 0;
        List<EventError> rejected = new ArrayList<>();
        List<EventError> failed = new ArrayList<>();

        for (int index = 0; index < items.size(); index++) {
            JsonNode item = items.get(index);

            EventValidator.Result validation = eventValidator.validate(item);
            if (!validation.isValid()) {
                rejected.add(new EventError(index, readEventId(item), validation.errors()));
                continue;
            }

            EventRequest request = validation.request();
            String eventId = request.eventId();

            // Senders retry: skip repeats instead of overwriting
            if (seenIds.contains(eventId) || eventRepository.existsById(eventId)) {
                duplicates++;
                continue;
            }
            seenIds.add(eventId);

            Event event = toEvent(request);
            enrich(event, receivedAt);

            try {
                eventRepository.save(event);
                ingested++;
            } catch (CannotCreateTransactionException | DataAccessResourceFailureException ex) {
                throw ex;   // DB unreachable: stop, the handler answers 503
            } catch (DataAccessException ex) {
                log.error("Could not store event {}", eventId, ex);
                failed.add(new EventError(index, eventId, List.of("could not be stored")));
            }
        }

        return new IngestResult(items.size(), ingested, duplicates, rejected, failed);
    }

    private void enrich(Event event, Instant receivedAt) {
        Category category = event.getRule().category();

        event.setReceivedAt(receivedAt);
        event.setAttackType(attackClassifier.classify(category));
        // v0.2: compute risk score from severity, category, and attack type
    }

    private Event toEvent(EventRequest request) {
        Event event = new Event();
        event.setEventId(request.eventId());
        event.setTimestamp(request.timestamp());
        event.setConfigId(request.configId());
        event.setPolicyId(request.policyId());
        event.setClientIp(request.clientIp());
        event.setHostname(request.hostname());
        event.setPath(request.path());
        event.setMethod(request.method());
        event.setStatusCode(request.statusCode());
        event.setUserAgent(request.userAgent());
        event.setAction(request.action());
        event.setRequestSize(request.requestSize());
        event.setResponseSize(request.responseSize());
        event.setRule(toRule(request.rule()));
        event.setGeoLocation(toGeoLocation(request.geoLocation()));
        return event;
    }

    private Rule toRule(RuleRequest rule) {
        return new Rule(rule.id(), rule.name(), rule.message(), rule.severity(), rule.category());
    }

    private GeoLocation toGeoLocation(GeoLocationRequest geo) {
        if (geo == null) {
            return null;   
        }
        return new GeoLocation(geo.country(), geo.city());
    }

    // eventId of an invalid event, for the error report (null if unreadable)
    private String readEventId(JsonNode item) {
        return item.path("eventId").stringValue(null);
    }
}
