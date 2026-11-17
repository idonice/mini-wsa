package com.idonice.mini_wsa.controller;

import com.idonice.mini_wsa.dto.stats.StatsSummaryResult;
import com.idonice.mini_wsa.service.EventStatsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/v1/stats")
public class EventStatsController {

    private final EventStatsService statsService;

    public EventStatsController(EventStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/summary")
    public StatsSummaryResult summary(@RequestParam(required = false) Long configId,
                                      @RequestParam Instant from,
                                      @RequestParam Instant to) {
        if (!from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be before 'to'");
        }
        return statsService.getSummary(configId, from, to);
    }
}
