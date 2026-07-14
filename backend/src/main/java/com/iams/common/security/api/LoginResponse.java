package com.iams.common.security.api;

public record LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
