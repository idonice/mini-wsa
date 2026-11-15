package com.idonice.mini_wsa.dto;

import com.idonice.mini_wsa.model.Category;
import com.idonice.mini_wsa.model.Severity;
import jakarta.validation.constraints.NotNull;

public record RuleRequest(
        String id,
        String name,
        String message,
        @NotNull Severity severity,
        @NotNull Category category
) {}
