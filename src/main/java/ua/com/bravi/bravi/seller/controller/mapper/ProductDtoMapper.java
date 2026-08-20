package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.seller.catalog.products.api.ImageUpload;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductImageView;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductPage;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductView;
import ua.com.bravi.bravi.seller.catalog.products.domain.Product;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductImageUploadUrlRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductImageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductImageUploadUrlResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductPageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductResponse;
import ua.com.bravi.bravi.shared.media.PresignedUpload;

import java.util.List;

@Mapper(componentModel = "spring", uses = {AttributeDtoMapper.class, DiscountDtoMapper.class})
public interface ProductDtoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toDomain(ProductCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toDomain(ProductUpdateRequest request);

    ProductResponse toResponse(ProductView view);

    ProductImageResponse toImageResponse(ProductImageView image);

    List<ProductImageResponse> toImageResponses(List<ProductImageView> images);

    ProductPageResponse toPageResponse(ProductPage page);

    @Mapping(target = "size", source = "fileSize")
    @Mapping(target = "originalFilename", source = "filename")
    ImageUpload toUpload(ProductImageUploadUrlRequest request);

    @Mapping(target = "headers", source = "requiredHeaders")
    ProductImageUploadUrlResponse toUploadUrlResponse(PresignedUpload presigned);
}
