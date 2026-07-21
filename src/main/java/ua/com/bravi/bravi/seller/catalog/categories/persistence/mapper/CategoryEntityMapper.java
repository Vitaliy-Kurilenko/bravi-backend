package ua.com.bravi.bravi.seller.catalog.categories.persistence.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoryView;
import ua.com.bravi.bravi.seller.catalog.categories.domain.Category;
import ua.com.bravi.bravi.seller.catalog.categories.persistence.entity.CategoryEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryEntityMapper {

    @Mapping(target = "parentPublicId", ignore = true)
    Category toDomain(CategoryEntity entity);

    List<Category> toDomain(List<CategoryEntity> entities);

    @Mapping(target = "parentPublicId", source = "parentPublicId")
    @Mapping(target = "children", source = "children")
    CategoryView toView(CategoryEntity entity, String parentPublicId, List<CategoryView> children);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "parentId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CategoryEntity toEntity(Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "parentId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget CategoryEntity entity, Category patch);
}
