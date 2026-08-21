package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductTagRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.TagCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.TagUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductTagsBulkResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.TagPageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.TagResponse;
import ua.com.bravi.bravi.seller.tags.api.TagPage;
import ua.com.bravi.bravi.seller.tags.api.TagView;
import ua.com.bravi.bravi.seller.tags.domain.Tag;
import ua.com.bravi.bravi.seller.tags.domain.TagRef;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagDtoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "target", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Tag toDomain(TagCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "target", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Tag toDomain(TagUpdateRequest request);

    TagRef toDomain(ProductTagRequest request);

    List<TagRef> toDomains(List<ProductTagRequest> requests);

    TagResponse toResponse(TagView view);

    List<TagResponse> toResponses(List<TagView> views);

    TagPageResponse toPageResponse(TagPage page);

    default ProductTagsBulkResponse toBulkResponse(int updated) {
        return new ProductTagsBulkResponse(updated);
    }
}
