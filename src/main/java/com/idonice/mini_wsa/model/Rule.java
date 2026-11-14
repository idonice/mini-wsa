package com.idonice.mini_wsa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public record Rule(
        @Column(name = "rule_id") String id,
        @Column(name = "rule_name") String name,
        @Column(name = "rule_message") String message,
        @Enumerated(EnumType.STRING) @Column(name = "rule_severity") Severity severity,
        @Enumerated(EnumType.STRING) @Column(name = "rule_category") Category category
) {}
