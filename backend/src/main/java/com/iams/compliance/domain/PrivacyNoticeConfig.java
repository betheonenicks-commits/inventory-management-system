package com.iams.compliance.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** US-CMP-03: privacy-notice text attached to a personal-data field, shown at the point of capture. */
@Getter
@Setter
@Entity
@Table(name = "privacy_notice_config")
public class PrivacyNoticeConfig extends BaseEntity {

    @Column(name = "field_name", nullable = false, unique = true)
    private String fieldName;

    @Column(name = "notice_text", nullable = false, length = 1000)
    private String noticeText;
}
