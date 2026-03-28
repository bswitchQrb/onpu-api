package com.onpu.dto;

import java.util.Map;

public record StatsResponse(
    int totalAnswers,
    int totalCorrect,
    Map<String, ModeStats> byMode
) {
    public record ModeStats(int answers, int correct) {}
}
