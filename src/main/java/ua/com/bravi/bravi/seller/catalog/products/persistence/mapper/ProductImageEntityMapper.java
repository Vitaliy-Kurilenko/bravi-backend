package ua.com.bravi.bravi.seller.catalog.products.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductImageView;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductImageEntity;

@Mapper(componentModel = "spring")
public interface ProductImageEntityMapper {

    @Mapping(target = "url", source = "url")
    ProductImageView toView(ProductImageEntity entity, String url);
}
