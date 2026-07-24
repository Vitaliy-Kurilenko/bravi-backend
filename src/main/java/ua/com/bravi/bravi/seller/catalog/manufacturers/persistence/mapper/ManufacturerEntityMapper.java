package ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturerView;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.Manufacturer;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.entity.ManufacturerEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ManufacturerEntityMapper {

    Manufacturer toDomain(ManufacturerEntity entity);

    ManufacturerView toView(ManufacturerEntity entity);

    List<ManufacturerView> toViews(List<ManufacturerEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ManufacturerEntity toEntity(Manufacturer manufacturer);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget ManufacturerEntity entity, Manufacturer patch);
}
