package com.idonice.mini_wsa.dto.event;

import com.idonice.mini_wsa.model.Action;
import com.idonice.mini_wsa.model.Event;
import com.idonice.mini_wsa.model.GeoLocation;
import com.idonice.mini_wsa.model.Rule;

import java.time.Instant;

public record EnrichedEvent(
        String eventId,
        Instant timestamp,
        Long configId,
        String policyId,
        String clientIp,
        String hostname,
        String path,
        String method,
        Integer statusCode,
        String userAgent,
        Rule rule,
        Action action,
        GeoLocation geoLocation,
        Long requestSize,
        Long responseSize,
        String attackType,
        int threatScore,
        Instant receivedAt
) {

    public static EnrichedEvent from(Event event) {
        return new EnrichedEvent(
                event.getEventId(),
                event.getTimestamp(),
                event.getConfigId(),
                event.getPolicyId(),
                event.getClientIp(),
                event.getHostname(),
                event.getPath(),
                event.getMethod(),
                event.getStatusCode(),
                event.getUserAgent(),
                event.getRule(),
                event.getAction(),
                event.getGeoLocation(),
                event.getRequestSize(),
                event.getResponseSize(),
                event.getAttackType(),
                event.getThreatScore(),
                event.getReceivedAt());
    }
}
