package com.kisandaily.domain;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String userId, UUID sessionId) {}
