package ua.com.bravi.bravi.seller.tags.persistence.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ua.com.bravi.bravi.seller.tags.api.TagView;
import ua.com.bravi.bravi.seller.tags.domain.Tag;
import ua.com.bravi.bravi.seller.tags.persistence.entity.TagEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagEntityMapper {

    @Mapping(target = "usageCount", source = "usageCount")
    TagView toView(TagEntity entity, Long usageCount);

    default TagView toView(TagEntity entity) {
        return toView(entity, null);
    }

    List<TagView> toViews(List<TagEntity> entities);

    Tag toDomain(TagEntity entity);

    List<Tag> toDomains(List<TagEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "target", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget TagEntity entity, Tag patch);
}
