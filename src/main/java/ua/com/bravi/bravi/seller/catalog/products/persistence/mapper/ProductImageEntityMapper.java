package ua.com.bravi.bravi.seller.catalog.products.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductImageView;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductGallery;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductImageEntity;

@Mapper(componentModel = "spring", imports = ProductGallery.class)
public interface ProductImageEntityMapper {

    @Mapping(target = "url", source = "url")
    @Mapping(target = "isPrimary", expression = "java(ProductGallery.isPrimary(entity.getSortOrder()))")
    ProductImageView toView(ProductImageEntity entity, String url);
}
