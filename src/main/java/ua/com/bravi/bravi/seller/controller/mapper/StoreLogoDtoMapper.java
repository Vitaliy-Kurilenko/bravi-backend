package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.seller.controller.dto.in.LogoUploadUrlRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.LogoUploadUrlResponse;
import ua.com.bravi.bravi.seller.stores.api.LogoUpload;
import ua.com.bravi.bravi.shared.media.PresignedUpload;

@Mapper(componentModel = "spring")
public interface StoreLogoDtoMapper {

    @Mapping(target = "size", source = "fileSize")
    @Mapping(target = "originalFilename", source = "filename")
    LogoUpload toUpload(LogoUploadUrlRequest request);

    @Mapping(target = "headers", source = "requiredHeaders")
    LogoUploadUrlResponse toResponse(PresignedUpload presigned);
}
