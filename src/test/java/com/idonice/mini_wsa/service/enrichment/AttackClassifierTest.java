package com.idonice.mini_wsa.service.enrichment;

import com.idonice.mini_wsa.model.Category;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AttackClassifierTest {

    private final AttackClassifier classifier = new AttackClassifier();

    @ParameterizedTest
    @CsvSource({
            "INJECTION,          SQL/Command Injection",
            "XSS,                Cross-Site Scripting",
            "PROTOCOL_VIOLATION, Protocol Anomaly",
            "DATA_LEAKAGE,       Data Exfiltration",
            "BOT,                Bot Activity",
            "DOS,                Denial of Service",
            "RATE_LIMIT,         Rate Limiting"
    })
    void mapsEachCategoryToItsAttackType(Category category, String expectedAttackType) {
        assertThat(classifier.classify(category)).isEqualTo(expectedAttackType);
    }
}
