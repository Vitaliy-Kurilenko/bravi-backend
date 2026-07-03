package ua.com.bravi.bravi.access.persistence.mapper;

import org.mapstruct.Mapper;
import ua.com.bravi.bravi.access.api.AccountView;
import ua.com.bravi.bravi.access.domain.Account;
import ua.com.bravi.bravi.access.persistence.entity.AccountEntity;

@Mapper(componentModel = "spring")
public interface AccountEntityMapper {

    Account toDomain(AccountEntity entity);

    AccountView toView(Account account);
}
