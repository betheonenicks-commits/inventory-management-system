package com.iams.storage.infrastructure;

import com.iams.storage.application.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The one place that talks to MinIO (US-PLAT-02): everything else goes
 * through AttachmentService, so authorization always happens before a byte
 * moves in either direction. Bucket creation is lazy-but-idempotent on first
 * use rather than a startup step - the app must not fail to boot because the
 * object store is momentarily unavailable while no one is uploading anything.
 */
@Component
public class ObjectStorageClient {

    private static final Logger log = LoggerFactory.getLogger(ObjectStorageClient.class);

    private final MinioClient client;
    private final String bucket;
    private volatile boolean bucketEnsured;

    public ObjectStorageClient(StorageProperties properties) {
        this.client = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
        this.bucket = properties.getBucket();
    }

    public void put(String key, byte[] content, String contentType) {
        ensureBucket();
        try (InputStream in = new ByteArrayInputStream(content)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(key)
                    .stream(in, content.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new StorageUnavailableException("Object-store write failed for key " + key, e);
        }
    }

    /** Caller must close the stream. */
    public InputStream get(String key) {
        ensureBucket();
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new StorageUnavailableException("Object-store read failed for key " + key, e);
        }
    }

    public void delete(String key) {
        ensureBucket();
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new StorageUnavailableException("Object-store delete failed for key " + key, e);
        }
    }

    /** Keys of objects last modified before the cutoff - the janitor's raw candidate list. */
    public List<String> listKeysOlderThan(Instant cutoff) {
        ensureBucket();
        List<String> keys = new ArrayList<>();
        try {
            for (Result<Item> result : client.listObjects(
                    ListObjectsArgs.builder().bucket(bucket).recursive(true).build())) {
                Item item = result.get();
                if (item.lastModified() != null && item.lastModified().toInstant().isBefore(cutoff)) {
                    keys.add(item.objectName());
                }
            }
        } catch (Exception e) {
            throw new StorageUnavailableException("Object-store listing failed", e);
        }
        return keys;
    }

    private void ensureBucket() {
        if (bucketEnsured) {
            return;
        }
        synchronized (this) {
            if (bucketEnsured) {
                return;
            }
            try {
                if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Created object-store bucket '{}'", bucket);
                }
                bucketEnsured = true;
            } catch (Exception e) {
                throw new StorageUnavailableException("Object store unreachable while ensuring bucket '" + bucket + "'", e);
            }
        }
    }

    /** Read fully and close - for callers that need bytes, not a stream. */
    public byte[] getBytes(String key) {
        try (InputStream in = get(key)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new StorageUnavailableException("Object-store read failed for key " + key, e);
        }
    }
}
