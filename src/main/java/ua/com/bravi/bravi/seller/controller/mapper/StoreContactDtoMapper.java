package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreContactCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreContactUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreContactResponse;
import ua.com.bravi.bravi.seller.stores.contacts.api.StoreContactView;
import ua.com.bravi.bravi.seller.stores.contacts.domain.StoreContact;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StoreContactDtoMapper {

    StoreContactResponse toResponse(StoreContactView contact);

    List<StoreContactResponse> toResponses(List<StoreContactView> contacts);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StoreContact toDomain(StoreContactCreateRequest request);

    List<StoreContact> toDomainFromCreate(List<StoreContactCreateRequest> requests);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StoreContact toDomain(StoreContactUpdateRequest request);
}
