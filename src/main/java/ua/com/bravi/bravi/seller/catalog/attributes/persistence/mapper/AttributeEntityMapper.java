package ua.com.bravi.bravi.seller.catalog.attributes.persistence.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributeOptionView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributeTemplateView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributeView;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.Attribute;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeEntity;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeTemplateEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttributeEntityMapper {

    Attribute toDomain(AttributeEntity entity);

    AttributeView toView(AttributeEntity entity, List<AttributeOptionView> options);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "templateCode", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AttributeEntity toEntity(Attribute attribute);

    /** Code and value type are fixed after creation, so a patch never reaches them. */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "templateCode", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "valueType", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget AttributeEntity entity, Attribute patch);

    @Mapping(target = "adopted", source = "adopted")
    @Mapping(target = "options", source = "options")
    AttributeTemplateView toTemplateView(AttributeTemplateEntity entity, boolean adopted,
                                         List<AttributeOptionView> options);

    /** Seeds a store-owned definition from a library template, keeping the template code as the mapping key. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "templateCode", source = "code")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "scope", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AttributeEntity fromTemplate(AttributeTemplateEntity template);
}
