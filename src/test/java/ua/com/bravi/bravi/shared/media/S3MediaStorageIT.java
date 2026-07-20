package ua.com.bravi.bravi.shared.media;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.MinIOContainer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import ua.com.bravi.bravi.shared.config.props.MediaStorageProperties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3MediaStorageIT {

    private static final String BUCKET = "bravi-media";

    private final MinIOContainer minio = new MinIOContainer("minio/minio:latest");

    private S3MediaStorage storage;
    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @BeforeAll
    void setUp() {
        minio.start();

        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setEndpoint(minio.getS3URL());
        properties.setRegion("us-east-1");
        properties.setBucket(BUCKET);
        properties.setAccessKey(minio.getUserName());
        properties.setSecretKey(minio.getPassword());
        properties.setPathStyleAccess(true);
        properties.setPublicBaseUrl(minio.getS3URL() + "/" + BUCKET);

        S3StorageConfig config = new S3StorageConfig();
        s3Client = config.s3Client(properties);
        s3Presigner = config.s3Presigner(properties);
        s3Client.createBucket(b -> b.bucket(BUCKET));

        storage = new S3MediaStorage(s3Client, s3Presigner, properties);
    }

    @AfterAll
    void tearDown() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
        minio.stop();
    }

    @Test
    void presignUploadThenStatThenDelete() throws Exception {
        byte[] content = "fake-png-bytes".getBytes(StandardCharsets.UTF_8);
        PresignedUpload presigned = storage.presignUpload(
                new MediaUploadRequest(MediaCategory.STORE_LOGO, "1", "image/png", content.length, "logo.png"));

        assertThat(presigned.storageKey()).startsWith("store-logos/1/").endsWith(".png");
        assertThat(presigned.requiredHeaders()).containsEntry("Content-Type", "image/png");

        HttpRequest.Builder put = HttpRequest.newBuilder(URI.create(presigned.uploadUrl()))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content));
        presigned.requiredHeaders().forEach(put::header);
        HttpResponse<Void> response = HttpClient.newHttpClient()
                .send(put.build(), HttpResponse.BodyHandlers.discarding());
        assertThat(response.statusCode()).isEqualTo(200);

        StoredObject stored = storage.stat(presigned.storageKey()).orElseThrow();
        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.size()).isEqualTo(content.length);

        assertThat(storage.publicUrl(presigned.storageKey()))
                .isEqualTo(minio.getS3URL() + "/" + BUCKET + "/" + presigned.storageKey());

        storage.delete(presigned.storageKey());
        assertThat(storage.stat(presigned.storageKey())).isEmpty();
    }

    @Test
    void listReturnsOnlyKeysUnderThePrefix() throws Exception {
        String own = upload("2", "own.png");
        String other = upload("3", "other.png");

        assertThat(storage.list("store-logos/2/")).containsExactly(own);
        assertThat(storage.list("store-logos/2/")).doesNotContain(other);
        assertThat(storage.list("store-logos/nothing-here/")).isEmpty();
    }

    /** Presigns and actually PUTs an object, returning its storage key. */
    private String upload(String scope, String filename) throws Exception {
        byte[] content = "fake-png-bytes".getBytes(StandardCharsets.UTF_8);
        PresignedUpload presigned = storage.presignUpload(
                new MediaUploadRequest(MediaCategory.STORE_LOGO, scope, "image/png", content.length, filename));
        HttpRequest.Builder put = HttpRequest.newBuilder(URI.create(presigned.uploadUrl()))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content));
        presigned.requiredHeaders().forEach(put::header);
        HttpClient.newHttpClient().send(put.build(), HttpResponse.BodyHandlers.discarding());
        return presigned.storageKey();
    }
}
