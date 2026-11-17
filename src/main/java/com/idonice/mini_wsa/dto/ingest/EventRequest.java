package com.idonice.mini_wsa.dto.ingest;

import com.idonice.mini_wsa.model.Action;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EventRequest(
        @NotBlank @Size(max = 100) String eventId,
        @NotNull Instant timestamp,
        @NotNull @Positive Long configId,
        @Size(max = 100) String policyId,

        @NotBlank @Size(max = 45)
        @Pattern(regexp = "^[0-9A-Fa-f:.]+$", message = "must be an IPv4 or IPv6 address")
        String clientIp,

        @Size(max = 255) String hostname,
        @NotBlank @Size(max = 2048) String path,
        @Size(max = 10) String method,
        Integer statusCode,
        @Size(max = 1024) String userAgent,
        @NotNull @Valid RuleRequest rule,
        @NotNull Action action,
        @Valid GeoLocationRequest geoLocation,
        Long requestSize,
        Long responseSize
) {}
