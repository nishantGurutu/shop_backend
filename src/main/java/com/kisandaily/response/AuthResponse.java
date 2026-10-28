package com.kisandaily.response;

import java.time.Instant;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    UserResponse user) {}
