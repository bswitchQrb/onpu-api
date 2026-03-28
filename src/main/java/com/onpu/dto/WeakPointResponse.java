package com.onpu.dto;

public record WeakPointResponse(
    String question,
    int answers,
    int correct,
    double rate
) {}
