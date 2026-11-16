package com.idonice.mini_wsa.dto;

import java.util.List;

public record EventError(
        int index,
        String eventId,
        List<String> errors
) {}
