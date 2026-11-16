package com.idonice.mini_wsa.dto;

import com.idonice.mini_wsa.model.Category;
import com.idonice.mini_wsa.model.Severity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RuleRequest(
        @Size(max = 50) String id,
        @Size(max = 100) String name,
        @Size(max = 500) String message,
        @NotNull Severity severity,
        @NotNull Category category
) {}
