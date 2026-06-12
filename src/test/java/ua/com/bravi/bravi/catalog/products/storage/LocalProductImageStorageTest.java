package ua.com.bravi.bravi.catalog.products.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.com.bravi.bravi.catalog.products.config.props.ProductImageStorageProperties;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalProductImageStorageTest {

    private LocalProductImageStorage storage(Path dir) {
        ProductImageStorageProperties props = new ProductImageStorageProperties();
        props.setBasePath(dir.toString());
        return new LocalProductImageStorage(props);
    }

    @Test
    void storeLoadDeleteRoundTrip(@TempDir Path dir) {
        LocalProductImageStorage storage = storage(dir);
        byte[] content = {1, 2, 3, 4};

        String key = storage.store(content, "image/png", "photo.png");

        assertThat(key).endsWith(".png");
        assertThat(storage.load(key)).isEqualTo(content);

        storage.delete(key);
        assertThatThrownBy(() -> storage.load(key)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteIsIdempotent(@TempDir Path dir) {
        LocalProductImageStorage storage = storage(dir);
        storage.delete("nonexistent.png"); // не кидає
    }

    @Test
    void derivesExtensionFromContentTypeWhenFilenameHasNone(@TempDir Path dir) {
        String key = storage(dir).store(new byte[]{9}, "image/jpeg", "noext");
        assertThat(key).endsWith(".jpg");
    }
}
