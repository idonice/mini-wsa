package com.idonice.mini_wsa.service;

import com.idonice.mini_wsa.model.Category;
import org.springframework.stereotype.Component;

@Component
public class AttackClassifier {
    public String classify(Category category) {
        return switch (category) {
            case INJECTION -> "SQL/Command Injection";
            case XSS -> "Cross-Site Scripting";
            case PROTOCOL_VIOLATION -> "Protocol Anomaly";
            case DATA_LEAKAGE -> "Data Exfiltration";
            case BOT -> "Bot Activity";
            case DOS -> "Denial of Service";
            case RATE_LIMIT -> "Rate Limiting";
        };
    }
}
