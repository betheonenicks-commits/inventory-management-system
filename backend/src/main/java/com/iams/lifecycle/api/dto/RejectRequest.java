package com.iams.lifecycle.api.dto;

import jakarta.validation.constraints.NotBlank;

/** US-LIF-11: shared reject shape for both transfer and disposal requests. */
public record RejectRequest(@NotBlank String reason) {
}
