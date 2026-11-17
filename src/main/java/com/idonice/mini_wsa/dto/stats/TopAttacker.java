package com.idonice.mini_wsa.dto.stats;

public record TopAttacker(
        String clientIp,
        Long count,
        Double avgThreatScore
) {}
