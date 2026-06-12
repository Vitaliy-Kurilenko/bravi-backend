package ua.com.bravi.bravi.catalog.products.persistence.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ua.com.bravi.bravi.catalog.products.api.ProductImageView;
import ua.com.bravi.bravi.catalog.products.api.ProductView;
import ua.com.bravi.bravi.catalog.products.domain.Product;
import ua.com.bravi.bravi.catalog.products.persistence.entity.ProductEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductEntityMapper {

    Product toDomain(ProductEntity entity);

    @Mapping(target = "images", source = "images")
    ProductView toView(ProductEntity entity, List<ProductImageView> images);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductEntity toEntity(Product product);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget ProductEntity entity, Product patch);
}
