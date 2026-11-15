package com.idonice.mini_wsa.dto;

import com.idonice.mini_wsa.model.Action;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

// The JSON a client sends to POST /v1/events/ingest.
// Required = the fields the scoring, classification and stats depend on.
public record EventRequest(
        @NotBlank String eventId,
        @NotNull Instant timestamp,
        @NotNull Long configId,
        String policyId,
        @NotBlank String clientIp,
        String hostname,
        @NotBlank String path,
        String method,
        Integer statusCode,
        String userAgent,
        @NotNull @Valid RuleRequest rule,
        @NotNull Action action,
        @Valid GeoLocationRequest geoLocation,
        Long requestSize,
        Long responseSize
) {}
