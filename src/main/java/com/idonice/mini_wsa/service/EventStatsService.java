package com.idonice.mini_wsa.service;

import com.idonice.mini_wsa.dto.stats.CategoryStats;
import com.idonice.mini_wsa.dto.stats.StatsSummaryResult;
import com.idonice.mini_wsa.dto.stats.TimeRange;
import com.idonice.mini_wsa.dto.stats.TopAttacker;
import com.idonice.mini_wsa.dto.stats.TopPath;
import com.idonice.mini_wsa.model.Action;
import com.idonice.mini_wsa.model.Category;
import com.idonice.mini_wsa.repository.EventStatsRepository;
import com.idonice.mini_wsa.repository.projection.CategoryActionCount;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class EventStatsService {

    private static final int TOP_N = 10;

    private final EventStatsRepository statsRepository;

    public EventStatsService(EventStatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    public StatsSummaryResult getSummary(Long configId, Instant from, Instant to) {
        List<CategoryActionCount> cells = statsRepository.countByCategoryAndAction(configId, from, to);

        return new StatsSummaryResult(
                configId,
                new TimeRange(from, to),
                totalEvents(cells),
                byCategory(cells),
                byAction(cells),
                topAttackers(configId, from, to),
                topPaths(configId, from, to));
    }

    private long totalEvents(List<CategoryActionCount> cells) {
        long total = 0;
        for (CategoryActionCount cell : cells) {
            total += cell.count();
        }
        return total;
    }

    // Adds up the cells per category, then average = score sum / count
    private Map<Category, CategoryStats> byCategory(List<CategoryActionCount> cells) {
        Map<Category, Long> counts = new EnumMap<>(Category.class);
        Map<Category, Long> scoreSums = new EnumMap<>(Category.class);

        for (CategoryActionCount cell : cells) {
            Category category = cell.category();
            counts.put(category, counts.getOrDefault(category, 0L) + cell.count());
            scoreSums.put(category, scoreSums.getOrDefault(category, 0L) + cell.scoreSum());
        }

        Map<Category, CategoryStats> result = new EnumMap<>(Category.class);
        for (Category category : counts.keySet()) {
            long count = counts.get(category);
            double average = (double) scoreSums.get(category) / count;
            result.put(category, new CategoryStats(count, round1(average)));
        }
        return result;
    }

    private Map<Action, Long> byAction(List<CategoryActionCount> cells) {
        Map<Action, Long> counts = new EnumMap<>(Action.class);
        for (CategoryActionCount cell : cells) {
            Action action = cell.action();
            counts.put(action, counts.getOrDefault(action, 0L) + cell.count());
        }
        return counts;
    }

    private List<TopAttacker> topAttackers(Long configId, Instant from, Instant to) {
        return statsRepository.findTopAttackers(configId, from, to, Limit.of(TOP_N)).stream()
                .map(attacker -> new TopAttacker(
                        attacker.clientIp(), attacker.count(), round1(attacker.avgThreatScore())))
                .toList();
    }

    private List<TopPath> topPaths(Long configId, Instant from, Instant to) {
        return statsRepository.findTopPaths(configId, from, to, Limit.of(TOP_N));
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
