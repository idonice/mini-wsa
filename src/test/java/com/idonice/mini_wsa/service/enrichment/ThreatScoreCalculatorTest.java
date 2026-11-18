package com.idonice.mini_wsa.service.enrichment;

import com.idonice.mini_wsa.model.Action;
import com.idonice.mini_wsa.model.Category;
import com.idonice.mini_wsa.model.Event;
import com.idonice.mini_wsa.model.Rule;
import com.idonice.mini_wsa.model.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ThreatScoreCalculatorTest {

    private static final String PLAIN_PATH = "/api/v1/products";

    private final ThreatScoreCalculator calculator = new ThreatScoreCalculator();

    @ParameterizedTest
    @CsvSource({"CRITICAL, 40", "HIGH, 30", "MEDIUM, 20", "LOW, 10"})
    void severityPoints(Severity severity, int expected) {
        Event event = event(severity, Action.MONITOR, PLAIN_PATH);
        assertThat(calculator.calculate(event, 0)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"DENY, 30", "ALERT, 20", "MONITOR, 10"})
    void actionPointsAreAddedToSeverity(Action action, int expected) {
        Event event = event(Severity.LOW, action, PLAIN_PATH);  
        assertThat(calculator.calculate(event, 0)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/admin/users", "/api/v1/login", "/login"})
    void sensitivePathAdds15(String path) {
        Event event = event(Severity.LOW, Action.MONITOR, path);
        assertThat(calculator.calculate(event, 0)).isEqualTo(10 + 15);
    }

    @Test
    void otherPathAddsNothing() {
        Event event = event(Severity.LOW, Action.MONITOR, PLAIN_PATH);
        assertThat(calculator.calculate(event, 0)).isEqualTo(10);
    }

    @Test
    void fiveRecentEventsIsNotARepeatOffender() {
        Event event = event(Severity.LOW, Action.MONITOR, PLAIN_PATH);
        assertThat(calculator.calculate(event, 5)).isEqualTo(10);
    }

    @Test
    void sixRecentEventsIsARepeatOffender() {
        Event event = event(Severity.LOW, Action.MONITOR, PLAIN_PATH);
        assertThat(calculator.calculate(event, 6)).isEqualTo(10 + 15);
    }

    @Test
    void allPartsTogetherGiveTheMaximumOf90() {
        Event event = event(Severity.CRITICAL, Action.DENY, "/admin");
        assertThat(calculator.calculate(event, 6)).isEqualTo(40 + 20 + 15 + 15);
    }

    private Event event(Severity severity, Action action, String path) {
        Event event = new Event();
        event.setRule(new Rule("950001", "TEST_RULE", "test", severity, Category.INJECTION));
        event.setAction(action);
        event.setPath(path);
        return event;
    }
}
