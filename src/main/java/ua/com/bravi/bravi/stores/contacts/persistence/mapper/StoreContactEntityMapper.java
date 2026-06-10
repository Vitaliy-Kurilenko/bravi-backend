package ua.com.bravi.bravi.stores.contacts.persistence.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ua.com.bravi.bravi.stores.contacts.api.StoreContactView;
import ua.com.bravi.bravi.stores.contacts.domain.StoreContact;
import ua.com.bravi.bravi.stores.contacts.persistence.entity.StoreContactEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StoreContactEntityMapper {

    StoreContact toDomain(StoreContactEntity entity);

    List<StoreContact> toDomain(List<StoreContactEntity> entities);

    StoreContactView toView(StoreContactEntity entity);

    List<StoreContactView> toViews(List<StoreContactEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StoreContactEntity toEntity(StoreContact contact);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget StoreContactEntity entity, StoreContact patch);
}
