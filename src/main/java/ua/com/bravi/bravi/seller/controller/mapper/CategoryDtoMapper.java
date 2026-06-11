package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.catalog.categories.api.CategoryView;
import ua.com.bravi.bravi.catalog.categories.domain.Category;
import ua.com.bravi.bravi.seller.controller.dto.in.CategoryCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.CategoryUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.CategoryResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryDtoMapper {

    CategoryResponse toResponse(CategoryView category);

    List<CategoryResponse> toResponses(List<CategoryView> categories);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toDomain(CategoryCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toDomain(CategoryUpdateRequest request);
}
