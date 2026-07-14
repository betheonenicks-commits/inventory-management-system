package com.iams.compliance.api.dto;

import java.util.UUID;

public record PrivacyNoticeConfigResponse(UUID id, long version, String fieldName, String noticeText) {
}
