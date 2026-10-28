package com.kisandaily.dto;

import jakarta.validation.constraints.*;

public record RefreshTokenRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "सही refresh token भेजें")
        String refreshToken) {}
