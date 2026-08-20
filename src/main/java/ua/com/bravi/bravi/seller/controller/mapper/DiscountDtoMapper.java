package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountBulkResultView;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountView;
import ua.com.bravi.bravi.seller.catalog.discounts.api.SkippedProductView;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.Discount;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.SubmittedDiscount;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductDiscountRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductDiscountsBulkRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.ActiveDiscountResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductDiscountResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductDiscountsBulkResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.SkippedProductResponse;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface DiscountDtoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Discount toDomain(ProductDiscountRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Discount toDomain(ProductDiscountsBulkRequest request);

    /** Keeps each entry's position so a validation error can address the row the seller typed. */
    default List<SubmittedDiscount> toSubmitted(List<ProductDiscountRequest> requests) {
        List<SubmittedDiscount> submitted = new ArrayList<>(requests.size());
        for (int index = 0; index < requests.size(); index++) {
            submitted.add(new SubmittedDiscount(index, toDomain(requests.get(index))));
        }
        return submitted;
    }

    ProductDiscountResponse toResponse(DiscountView view);

    List<ProductDiscountResponse> toResponses(List<DiscountView> views);

    ActiveDiscountResponse toActiveDiscountResponse(DiscountView view);

    @Mapping(target = "productId", source = "productPublicId")
    @Mapping(target = "conflictingDiscountId", source = "conflictingDiscountPublicId")
    SkippedProductResponse toSkippedResponse(SkippedProductView view);

    ProductDiscountsBulkResponse toBulkResponse(DiscountBulkResultView result);
}
