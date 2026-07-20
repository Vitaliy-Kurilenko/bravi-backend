package ua.com.bravi.bravi.shared.media;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.shared.media.exception.InvalidMediaUploadException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaCategoryTest {

    private static final long MAX = 5L * 1024 * 1024;

    @Test
    void keyPrefixCombinesPrefixAndScope() {
        assertThat(MediaCategory.STORE_LOGO.keyPrefix("7")).isEqualTo("store-logos/7");
    }

    @Test
    void acceptsAllowedTypeWithinSize() {
        assertThatCode(() -> MediaCategory.STORE_LOGO.validate("image/png", 1024))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsupportedType() {
        assertThatThrownBy(() -> MediaCategory.STORE_LOGO.validate("application/pdf", 1024))
                .isInstanceOf(InvalidMediaUploadException.class);
    }

    @Test
    void rejectsNullType() {
        assertThatThrownBy(() -> MediaCategory.STORE_LOGO.validate(null, 1024))
                .isInstanceOf(InvalidMediaUploadException.class);
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> MediaCategory.STORE_LOGO.validate("image/png", 0))
                .isInstanceOf(InvalidMediaUploadException.class);
    }

    @Test
    void rejectsOversizedFile() {
        assertThatThrownBy(() -> MediaCategory.STORE_LOGO.validate("image/png", MAX + 1))
                .isInstanceOf(InvalidMediaUploadException.class);
    }
}
