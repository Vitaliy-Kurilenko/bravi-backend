package ua.com.bravi.bravi.catalog.products.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "catalog.products.image-storage")
public class ProductImageStorageProperties {

    /** Базовий каталог локального сховища зображень. */
    private String basePath = "./data/product-images";

    /** Максимальний розмір одного зображення. */
    private DataSize maxFileSize = DataSize.ofMegabytes(5);
}
