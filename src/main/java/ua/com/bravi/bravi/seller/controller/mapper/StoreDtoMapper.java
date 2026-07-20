package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreResponse;
import ua.com.bravi.bravi.seller.stores.api.StoreView;
import ua.com.bravi.bravi.seller.stores.domain.Store;

@Mapper(componentModel = "spring")
public interface StoreDtoMapper {

    StoreResponse toResponse(StoreView store);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sellerAccountId", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Store toDomain(StoreCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sellerAccountId", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Store toDomain(StoreUpdateRequest request);
}
