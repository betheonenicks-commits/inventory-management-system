package com.iams.storage.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * US-PLAT-02 object-store wiring. Picked up via IamsApplication's
 * @ConfigurationPropertiesScan like every other *Properties class here.
 * Credentials default to the MinIO root account the compose file provisions -
 * acceptable for a single-tenant appliance today; a dedicated scoped service
 * account is US-SEC-14/15 territory, not this story's.
 */
@ConfigurationProperties(prefix = "iams.storage")
public class StorageProperties {

    private String endpoint = "http://localhost:9000";
    private String accessKey = "iams-minio-bootstrap";
    private String secretKey = "iams_dev_minio_password";
    private String bucket = "iams-attachments";

    /** Server-side upload cap, enforced before any object-store write (AC-PLAT-02). */
    private long maxSizeBytes = 10 * 1024 * 1024;

    /** How old an object without a metadata row must be before the janitor reaps it. */
    private int janitorMinAgeHours = 24;

    /** Delay between janitor sweeps, milliseconds. */
    private long janitorDelayMs = 60 * 60 * 1000;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public int getJanitorMinAgeHours() {
        return janitorMinAgeHours;
    }

    public void setJanitorMinAgeHours(int janitorMinAgeHours) {
        this.janitorMinAgeHours = janitorMinAgeHours;
    }

    public long getJanitorDelayMs() {
        return janitorDelayMs;
    }

    public void setJanitorDelayMs(long janitorDelayMs) {
        this.janitorDelayMs = janitorDelayMs;
    }
}
