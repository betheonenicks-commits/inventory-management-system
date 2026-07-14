package com.iams.asset.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record VehicleDetailResponse(
        UUID id,
        long version,
        UUID assetId,
        String vin,
        String registrationNumber,
        Integer odometerReading,
        String odometerUnit,
        LocalDate registrationExpiryDate,
        LocalDate insuranceExpiryDate
) {
}
