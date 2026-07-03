package ua.com.bravi.bravi.seller.account.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.seller.account.api.SellerAccountView;
import ua.com.bravi.bravi.seller.account.persistence.entity.SellerAccountEntity;

@Mapper(componentModel = "spring")
public interface SellerAccountEntityMapper {

    @Mapping(target = "accountId", source = "entity.accountId")
    @Mapping(target = "accountPublicId", source = "accountPublicId")
    @Mapping(target = "onboardingStatus",
            expression = "java(entity.getOnboardingStatus() == null ? null : entity.getOnboardingStatus().name())")
    SellerAccountView toView(SellerAccountEntity entity, String accountPublicId);
}
