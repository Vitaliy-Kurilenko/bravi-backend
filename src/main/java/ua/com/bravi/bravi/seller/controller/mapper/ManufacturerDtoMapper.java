package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturerView;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.Manufacturer;
import ua.com.bravi.bravi.seller.controller.dto.in.ManufacturerCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ManufacturerUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.ManufacturerResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ManufacturerDtoMapper {

    ManufacturerResponse toResponse(ManufacturerView manufacturer);

    List<ManufacturerResponse> toResponses(List<ManufacturerView> manufacturers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Manufacturer toDomain(ManufacturerCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Manufacturer toDomain(ManufacturerUpdateRequest request);
}
