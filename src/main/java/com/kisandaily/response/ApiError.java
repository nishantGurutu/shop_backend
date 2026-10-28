package com.kisandaily.response;

import java.util.Map;

public record ApiError(String code, String message, Map<String, String> fields) {}
