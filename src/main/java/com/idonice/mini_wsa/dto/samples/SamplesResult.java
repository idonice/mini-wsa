package com.idonice.mini_wsa.dto.samples;

import com.idonice.mini_wsa.dto.event.EnrichedEvent;

import java.util.List;

public record SamplesResult(
        long total,                 
        int limit,
        int offset,
        List<EnrichedEvent> events
) {}
