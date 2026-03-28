package com.onpu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnswerRequest(
    @NotBlank String mode,
    @NotBlank String question,
    @NotNull Boolean isCorrect
) {}
