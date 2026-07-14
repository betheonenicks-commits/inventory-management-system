package com.iams.procurement.api.dto;

import jakarta.validation.constraints.Positive;

public record ReceiveLineRequest(@Positive int quantity, String discrepancyNote) {
}
