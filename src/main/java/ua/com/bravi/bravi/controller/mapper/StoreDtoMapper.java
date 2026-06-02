package ua.com.bravi.bravi.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.controller.dto.in.StoreCreateRequest;
import ua.com.bravi.bravi.controller.dto.in.StoreUpdateRequest;
import ua.com.bravi.bravi.controller.dto.out.StoreResponse;
import ua.com.bravi.bravi.domain.store.Store;

@Mapper(componentModel = "spring")
public interface StoreDtoMapper {

    StoreResponse toResponse(Store store);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sellerId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Store toDomain(StoreCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sellerId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Store toDomain(StoreUpdateRequest request);
}
