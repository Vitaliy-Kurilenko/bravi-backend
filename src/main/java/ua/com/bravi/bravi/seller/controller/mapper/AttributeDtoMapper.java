package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributeOptionView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributePage;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributeTemplateView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributeView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.CategoryAttributeView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.ProductAttributeValueView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.ProductAttributesView;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.Attribute;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeOption;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValue;
import ua.com.bravi.bravi.seller.controller.dto.in.AttributeCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.AttributeOptionRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.AttributeUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductAttributeValueRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.AttributeOptionResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.AttributePageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.AttributeResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.AttributeTemplateResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.CategoryAttributeResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductAttributeValueResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductAttributesResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttributeDtoMapper {

    AttributeResponse toResponse(AttributeView attribute);

    AttributeOptionResponse toResponse(AttributeOptionView option);

    List<AttributeOptionResponse> toOptionResponses(List<AttributeOptionView> options);

    AttributePageResponse toPageResponse(AttributePage page);

    AttributeTemplateResponse toResponse(AttributeTemplateView template);

    List<AttributeTemplateResponse> toTemplateResponses(List<AttributeTemplateView> templates);

    CategoryAttributeResponse toResponse(CategoryAttributeView categoryAttribute);

    List<CategoryAttributeResponse> toCategoryAttributeResponses(List<CategoryAttributeView> categoryAttributes);

    ProductAttributeValueResponse toResponse(ProductAttributeValueView value);

    List<ProductAttributeValueResponse> toValueResponses(List<ProductAttributeValueView> values);

    ProductAttributesResponse toResponse(ProductAttributesView attributes);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "templateCode", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Attribute toDomain(AttributeCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "templateCode", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "valueType", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Attribute toDomain(AttributeUpdateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "attributeId", ignore = true)
    @Mapping(target = "sortOrder", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    AttributeOption toDomain(AttributeOptionRequest request);

    List<AttributeOption> toOptionDomains(List<AttributeOptionRequest> requests);

    @Mapping(target = "attributePublicId", source = "attributeId")
    AttributeValue toDomain(ProductAttributeValueRequest request);

    List<AttributeValue> toValueDomains(List<ProductAttributeValueRequest> requests);
}
