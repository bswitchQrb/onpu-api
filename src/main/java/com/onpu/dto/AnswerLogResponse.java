package com.onpu.dto;

import java.time.LocalDateTime;

public record AnswerLogResponse(
    String mode,
    String question,
    boolean isCorrect,
    LocalDateTime answeredAt
) {}
