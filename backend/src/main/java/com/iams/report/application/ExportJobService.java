package com.iams.report.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.security.CurrentUserProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.stereotype.Service;

/**
 * US-RPT-12's background-export path: a very large export runs as a job with
 * progress instead of blocking the request. Jobs live in memory, own-rows-only
 * (another user's job id is a 404 - existence never leaks), and are evicted an
 * hour after creation - an export is a download artifact, not a record; this
 * deployment is a single-node appliance and the log says so plainly rather
 * than pretending an in-memory map is a distributed queue.
 */
@Service
public class ExportJobService {

    private static final Logger log = LoggerFactory.getLogger(ExportJobService.class);
    private static final Duration RETENTION = Duration.ofHours(1);

    public enum Status { RUNNING, COMPLETED, FAILED }

    public static final class ExportJob {
        private final UUID id = UUID.randomUUID();
        private final UUID ownerUserId;
        private final String reportKey;
        private final String format;
        private final Instant createdAt = Instant.now();
        private volatile Status status = Status.RUNNING;
        private volatile int progress;
        private volatile String fileName;
        private volatile byte[] content;
        private volatile String error;

        private ExportJob(UUID ownerUserId, String reportKey, String format) {
            this.ownerUserId = ownerUserId;
            this.reportKey = reportKey;
            this.format = format;
        }

        public UUID getId() {
            return id;
        }

        public String getReportKey() {
            return reportKey;
        }

        public String getFormat() {
            return format;
        }

        public Status getStatus() {
            return status;
        }

        public int getProgress() {
            return progress;
        }

        public String getFileName() {
            return fileName;
        }

        public String getError() {
            return error;
        }
    }

    private final Map<UUID, ExportJob> jobs = new ConcurrentHashMap<>();
    private final CurrentUserProvider currentUserProvider;
    private final Executor executor;

    public ExportJobService(CurrentUserProvider currentUserProvider,
                            @Qualifier("applicationTaskExecutor") Executor executor) {
        this.currentUserProvider = currentUserProvider;
        // Report generation applies org-scoping through the caller's security
        // context; the delegating wrapper captures it at execute() time on the
        // request thread so a scoped Department Head's background export sees
        // exactly what their foreground export would - never more.
        this.executor = new DelegatingSecurityContextExecutor(executor);
    }

    public ExportJob submit(String reportKey, String format, Supplier<TabularReport> reportSupplier,
                            RenderFunction renderer) {
        evictExpired();
        ExportJob job = new ExportJob(currentUserProvider.current().id(), reportKey, format);
        jobs.put(job.getId(), job);
        executor.execute(() -> run(job, reportSupplier, renderer));
        return job;
    }

    private void run(ExportJob job, Supplier<TabularReport> reportSupplier, RenderFunction renderer) {
        try {
            job.progress = 10;
            TabularReport report = reportSupplier.get();
            job.progress = 70;
            job.content = renderer.render(report);
            job.fileName = report.key() + "-" + java.time.LocalDate.now() + "." + job.format;
            job.progress = 100;
            job.status = Status.COMPLETED;
        } catch (Exception e) {
            job.status = Status.FAILED;
            job.error = "Export failed - see server logs";
            log.error("Export job {} ({} as {}) failed", job.getId(), job.getReportKey(), job.getFormat(), e);
        }
    }

    public ExportJob get(UUID id) {
        ExportJob job = jobs.get(id);
        if (job == null || !job.ownerUserId.equals(currentUserProvider.current().id())) {
            throw NotFoundException.of("ExportJob", id);
        }
        return job;
    }

    public DownloadableExport download(UUID id) {
        ExportJob job = get(id);
        if (job.status == Status.FAILED) {
            throw new ConflictException("EXPORT_FAILED", "This export failed and has nothing to download");
        }
        if (job.status != Status.COMPLETED) {
            throw new ConflictException("EXPORT_NOT_READY", "This export is still running - poll its status first");
        }
        return new DownloadableExport(job.fileName, job.format, job.content);
    }

    private void evictExpired() {
        Instant cutoff = Instant.now().minus(RETENTION);
        List<UUID> expired = jobs.values().stream()
                .filter(j -> j.createdAt.isBefore(cutoff))
                .map(ExportJob::getId)
                .toList();
        expired.forEach(jobs::remove);
    }

    public record DownloadableExport(String fileName, String format, byte[] content) {
    }

    @FunctionalInterface
    public interface RenderFunction {
        byte[] render(TabularReport report);
    }
}
