package com.onpu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 50) String loginId,
    @NotBlank @Size(min = 6) String password,
    @NotBlank @Size(max = 50) String nickname
) {}
