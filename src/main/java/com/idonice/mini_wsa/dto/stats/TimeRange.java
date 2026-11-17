package com.idonice.mini_wsa.dto.stats;

import java.time.Instant;

public record TimeRange(
        Instant from,
        Instant to
) {}
