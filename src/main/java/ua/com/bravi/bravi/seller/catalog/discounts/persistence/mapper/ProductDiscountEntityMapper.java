package ua.com.bravi.bravi.seller.catalog.discounts.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountView;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.Discount;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountStatus;
import ua.com.bravi.bravi.seller.catalog.discounts.persistence.entity.ProductDiscountEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductDiscountEntityMapper {

    /**
     * Identity and timestamps are assigned by the service, which carries them over from the row being
     * replaced so a resubmitted discount keeps its public id and creation time.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductDiscountEntity toEntity(Discount discount);

    Discount toDomain(ProductDiscountEntity entity);

    List<Discount> toDomains(List<ProductDiscountEntity> entities);

    DiscountView toView(Discount discount, DiscountStatus status);
}
