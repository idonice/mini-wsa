package com.idonice.mini_wsa.service;

import com.idonice.mini_wsa.model.Action;
import com.idonice.mini_wsa.model.Event;
import com.idonice.mini_wsa.model.Severity;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ThreatScoreCalculator {

    public static final Duration REPEAT_OFFENDER_WINDOW = Duration.ofMinutes(10);
    private static final int REPEAT_OFFENDER_THRESHOLD = 5;  
    private static final int PATH_BONUS = 15;
    private static final int REPEAT_OFFENDER_BONUS = 15;
    private static final int MAX_SCORE = 100;

    public int calculate(Event event, long recentEventsFromSameIp) {
        int score = severityPoints(event.getRule().severity())
                + actionPoints(event.getAction())
                + pathBonus(event.getPath())
                + repeatOffenderBonus(recentEventsFromSameIp);

        return Math.min(score, MAX_SCORE);
    }

    private int severityPoints(Severity severity) {
        return switch (severity) {
            case CRITICAL -> 40;
            case HIGH -> 30;
            case MEDIUM -> 20;
            case LOW -> 10;
        };
    }

    private int actionPoints(Action action) {
        return switch (action) {
            case DENY -> 20;
            case ALERT -> 10;
            case MONITOR -> 0;
        };
    }

    private int pathBonus(String path) {
        boolean sensitivePath = path.contains("/admin") || path.contains("/login");
        return sensitivePath ? PATH_BONUS : 0;
    }

    private int repeatOffenderBonus(long recentEventsFromSameIp) {
        boolean repeatOffender = recentEventsFromSameIp > REPEAT_OFFENDER_THRESHOLD;
        return repeatOffender ? REPEAT_OFFENDER_BONUS : 0;
    }
}
