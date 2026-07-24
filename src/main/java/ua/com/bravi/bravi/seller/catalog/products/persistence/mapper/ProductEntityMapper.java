package ua.com.bravi.bravi.seller.catalog.products.persistence.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductImageView;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductView;
import ua.com.bravi.bravi.seller.catalog.products.domain.Product;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductEntityMapper {

    // categoryId/manufacturerId у view — це public id'и суміжних агрегатів; ProductService їх резолвить
    // з internal bigint (entity) через categories/manufacturers api і передає сюди.
    @Mapping(target = "categoryId", source = "categoryPublicId")
    @Mapping(target = "manufacturerId", source = "manufacturerPublicId")
    @Mapping(target = "images", source = "images")
    ProductView toView(ProductEntity entity, String categoryPublicId, String manufacturerPublicId,
                       List<ProductImageView> images);

    // categoryId/manufacturerId приходять як public id — service резолвить їх у bigint і виставляє на entity.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "manufacturerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductEntity toEntity(Product product);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "manufacturerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget ProductEntity entity, Product patch);
}
