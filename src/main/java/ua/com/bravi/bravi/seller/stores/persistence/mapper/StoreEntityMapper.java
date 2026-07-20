package ua.com.bravi.bravi.seller.stores.persistence.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ua.com.bravi.bravi.seller.stores.api.StoreView;
import ua.com.bravi.bravi.seller.stores.domain.Store;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreEntity;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreSettingsEntity;

/**
 * Timezone and currency live in store_settings, not on the store row, so every mapping
 * that crosses the Store boundary either sources them from the settings entity or ignores them.
 */
@Mapper(componentModel = "spring")
public interface StoreEntityMapper {

    @Mapping(target = "timezone", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "allowReturn", ignore = true)
    @Mapping(target = "workingHours", ignore = true)
    Store toDomain(StoreEntity entity);

    @Mapping(target = "status", expression = "java(entity.getStatus() == null ? null : entity.getStatus().name())")
    @Mapping(target = "timezone", source = "settings.timezone")
    @Mapping(target = "currency", source = "settings.defaultCurrency")
    @Mapping(target = "allowReturn", source = "settings.allowReturn")
    @Mapping(target = "workingHours", source = "settings.workingHours")
    @Mapping(target = "createdAt", source = "entity.createdAt")
    @Mapping(target = "updatedAt", source = "entity.updatedAt")
    StoreView toView(StoreEntity entity, StoreSettingsEntity settings);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "sellerAccountId", ignore = true)
    @Mapping(target = "logoKey", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StoreEntity toEntity(Store store);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "sellerAccountId", ignore = true)
    @Mapping(target = "logoKey", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget StoreEntity entity, Store patch);

    /** Applies the settings-owned fields of a Store patch (timezone, currency, hours, returns). */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "defaultCurrency", source = "currency")
    @Mapping(target = "defaultLanguage", ignore = true)
    @Mapping(target = "defaultWeightUnit", ignore = true)
    @Mapping(target = "defaultDimensionUnit", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateSettings(@MappingTarget StoreSettingsEntity settings, Store patch);
}
