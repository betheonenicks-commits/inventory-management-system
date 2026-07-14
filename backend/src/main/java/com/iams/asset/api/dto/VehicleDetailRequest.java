package com.iams.asset.api.dto;

import java.time.LocalDate;

public record VehicleDetailRequest(
        String vin,
        String registrationNumber,
        Integer odometerReading,
        String odometerUnit,
        LocalDate registrationExpiryDate,
        LocalDate insuranceExpiryDate,
        Long version
) {
}
