package ua.com.bravi.bravi.seller.catalog.attributes.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributeOptionView;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeOptionEntity;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeTemplateOptionEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttributeOptionEntityMapper {

    AttributeOptionView toView(AttributeOptionEntity entity);

    List<AttributeOptionView> toViews(List<AttributeOptionEntity> entities);

    /** Library options carry no public id of their own; they get one once copied into a store. */
    @Mapping(target = "publicId", ignore = true)
    AttributeOptionView toView(AttributeTemplateOptionEntity entity);

    List<AttributeOptionView> toTemplateViews(List<AttributeTemplateOptionEntity> entities);
}
