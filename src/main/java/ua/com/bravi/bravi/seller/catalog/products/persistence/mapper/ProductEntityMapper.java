package ua.com.bravi.bravi.seller.catalog.products.persistence.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ua.com.bravi.bravi.seller.catalog.attributes.api.ProductAttributeValueView;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoryView;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturerView;
import ua.com.bravi.bravi.seller.catalog.products.api.CatalogRefView;
import ua.com.bravi.bravi.seller.catalog.discounts.api.ProductDiscountView;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductImageView;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductView;
import ua.com.bravi.bravi.seller.catalog.products.api.TagRefView;
import ua.com.bravi.bravi.seller.catalog.products.domain.Product;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductEntity;
import ua.com.bravi.bravi.seller.tags.api.TagView;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductEntityMapper {

    // In the view, category and manufacturer are nested references to neighbouring aggregates;
    // ProductService resolves them from the entity bigint ids and passes them in.
    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "name", source = "entity.name")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "manufacturer", source = "manufacturer")
    @Mapping(target = "images", source = "images")
    @Mapping(target = "attributes", source = "attributes")
    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "discountedPrice", source = "discount.discountedPrice")
    @Mapping(target = "activeDiscount", source = "discount.discount")
    ProductView toView(ProductEntity entity, CatalogRefView category, CatalogRefView manufacturer,
                       List<ProductImageView> images, List<ProductAttributeValueView> attributes,
                       List<TagRefView> tags, ProductDiscountView discount);

    @Mapping(target = "id", source = "publicId")
    CatalogRefView toRef(CategoryView category);

    @Mapping(target = "id", source = "publicId")
    CatalogRefView toRef(ManufacturerView manufacturer);

    // A tag rides along with the product as a bare reference: what the badge is drawn from, and
    // nothing else about it belongs on a product payload.
    @Mapping(target = "id", source = "publicId")
    TagRefView toTagRef(TagView tag);

    List<TagRefView> toTagRefs(List<TagView> tags);

    // categoryId and manufacturerId arrive as public ids; the service resolves them into bigints and sets them on the entity.
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
