package com.idonice.mini_wsa.dto.stats;

import com.idonice.mini_wsa.model.Action;
import com.idonice.mini_wsa.model.Category;

import java.util.List;
import java.util.Map;

public record StatsSummaryResult(
        Long configId,                        
        TimeRange timeRange,
        long totalEvents,
        Map<Category, CategoryStats> byCategory,
        Map<Action, Long> byAction,
        List<TopAttacker> topAttackers,
        List<TopPath> topTargetedPaths
) {}
