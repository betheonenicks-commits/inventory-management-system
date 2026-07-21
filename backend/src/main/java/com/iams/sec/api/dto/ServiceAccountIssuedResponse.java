package com.iams.sec.api.dto;

/**
 * US-SEC-14: the creation response. {@code apiKey} is the raw credential and is shown
 * exactly ONCE here - it is stored only as a hash (US-SEC-15) and can never be
 * retrieved again. The client must capture it now.
 */
public record ServiceAccountIssuedResponse(
        ServiceAccountResponse account,
        String apiKey
) {
}
