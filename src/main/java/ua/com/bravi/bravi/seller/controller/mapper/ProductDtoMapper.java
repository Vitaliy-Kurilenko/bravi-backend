package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.catalog.products.api.ProductImageView;
import ua.com.bravi.bravi.catalog.products.api.ProductPage;
import ua.com.bravi.bravi.catalog.products.api.ProductView;
import ua.com.bravi.bravi.catalog.products.domain.Product;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductImageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductPageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductDtoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toDomain(ProductCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toDomain(ProductUpdateRequest request);

    ProductResponse toResponse(ProductView view);

    ProductImageResponse toImageResponse(ProductImageView image);

    List<ProductImageResponse> toImageResponses(List<ProductImageView> images);

    ProductPageResponse toPageResponse(ProductPage page);
}
