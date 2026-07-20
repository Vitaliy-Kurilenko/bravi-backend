package ua.com.bravi.bravi.shared.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Інфраструктурні налаштування об'єктного сховища медіа (S3/MinIO) — тільки те, що варіюється між
 * середовищами. Розкладка ключів і обмеження на файли — не тут, а у {@link ua.com.bravi.bravi.shared.media.MediaCategory}.
 * Секрети — тільки через env.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bravi.media")
public class MediaStorageProperties {

    /** Endpoint сховища (для MinIO — локальний, для S3 — регіональний). */
    private String endpoint;

    private String region = "us-east-1";

    private String bucket;

    private String accessKey;

    private String secretKey;

    /** true для MinIO (path-style), false для реального S3 (virtual-hosted). */
    private boolean pathStyleAccess = true;

    /** База публічних URL об'єктів (bucket/CDN із public-read). */
    private String publicBaseUrl;

    /** TTL presigned-посилання на завантаження. */
    private Duration presignTtl = Duration.ofMinutes(10);
}
