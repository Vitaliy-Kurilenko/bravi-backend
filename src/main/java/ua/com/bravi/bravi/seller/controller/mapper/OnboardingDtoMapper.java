package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import ua.com.bravi.bravi.seller.controller.dto.in.OnboardingSettingsRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.OnboardingStorePatchRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.OnboardingStoreRequest;
import ua.com.bravi.bravi.seller.stores.api.StoreDraft;
import ua.com.bravi.bravi.seller.stores.api.StoreSettings;

@Mapper(componentModel = "spring")
public interface OnboardingDtoMapper {

    StoreDraft toDraft(OnboardingStoreRequest request);

    StoreDraft toDraft(OnboardingStorePatchRequest request);

    StoreSettings toSettings(OnboardingSettingsRequest request);
}
