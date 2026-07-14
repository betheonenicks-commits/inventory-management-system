package com.iams.compliance.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PrivacyNoticeConfigRequest(@NotBlank String fieldName, @NotBlank String noticeText) {
}
