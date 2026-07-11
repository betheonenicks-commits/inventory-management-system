package com.iams.common.security.api;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
