package ua.com.bravi.bravi.shared.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Infrastructure settings of the media object storage (S3/MinIO) — only what varies between
 * environments. Key layout and file limits belong to {@link ua.com.bravi.bravi.shared.media.MediaCategory}.
 * Secrets are supplied through environment variables.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bravi.media")
public class MediaStorageProperties {

    /** Storage endpoint: local for MinIO, regional for S3. */
    private String endpoint;

    private String region = "us-east-1";

    private String bucket;

    private String accessKey;

    private String secretKey;

    /** {@code true} for MinIO (path-style access), {@code false} for S3 (virtual-hosted). */
    private boolean pathStyleAccess = true;

    /** Base of public object URLs — a bucket or a CDN with public read access. */
    private String publicBaseUrl;

    /** Time to live of a presigned upload link. */
    private Duration presignTtl = Duration.ofMinutes(10);
}
