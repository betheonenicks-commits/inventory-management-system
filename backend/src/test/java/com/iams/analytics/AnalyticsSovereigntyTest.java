package com.iams.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * US-ANL-02's structural guarantee, executable: the analytics module's code
 * path contains no outbound network client of any kind, and no endpoint
 * through which a client could submit usage events. Scans the module's
 * actual sources so a future edit that adds either fails the build, not a
 * review. (Surefire's working directory is the backend module root.)
 */
class AnalyticsSovereigntyTest {

    private static final Path MODULE_ROOT = Path.of("src", "main", "java", "com", "iams", "analytics");

    /** Every outbound-capable client available on this classpath. */
    private static final List<String> FORBIDDEN_NETWORK_TOKENS = List.of(
            "java.net.http", "HttpURLConnection", "java.net.Socket", "java.net.URLConnection",
            "RestTemplate", "RestClient", "WebClient", "okhttp", "org.apache.http",
            "MinioClient", "JavaMailSender");

    @Test
    void analyticsModuleContainsNoOutboundNetworkCode() throws IOException {
        assertThat(MODULE_ROOT).isDirectory();
        try (Stream<Path> files = Files.walk(MODULE_ROOT)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String token : FORBIDDEN_NETWORK_TOKENS) {
                    assertThat(source).as("%s must not reference %s", file, token).doesNotContain(token);
                }
            }
        }
    }

    @Test
    void theOnlyWriteEndpointIsUserAuthoredFeedback_neverUsageSubmission() throws IOException {
        try (Stream<Path> files = Files.walk(MODULE_ROOT)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                boolean hasWriteEndpoint = source.contains("@PostMapping") || source.contains("@PutMapping")
                        || source.contains("@PatchMapping");
                if (hasWriteEndpoint) {
                    assertThat(file.getFileName().toString())
                            .as("only FeedbackController may expose a write endpoint in the analytics module")
                            .isEqualTo("FeedbackController.java");
                }
            }
        }
    }
}
