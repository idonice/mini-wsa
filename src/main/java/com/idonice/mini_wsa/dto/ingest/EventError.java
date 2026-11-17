package com.idonice.mini_wsa.dto.ingest;

import java.util.List;

public record EventError(
        int index,
        String eventId,
        List<String> errors
) {}
