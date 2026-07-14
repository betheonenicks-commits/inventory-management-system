package com.iams.procurement.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ReturnToVendorRequest(@Positive int quantity, @NotBlank String reason) {
}
