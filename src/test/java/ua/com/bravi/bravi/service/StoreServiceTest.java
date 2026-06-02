package ua.com.bravi.bravi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.component.InvocationContext;
import ua.com.bravi.bravi.domain.store.Store;
import ua.com.bravi.bravi.domain.store.StoreStatus;
import ua.com.bravi.bravi.domain.user.UserType;
import ua.com.bravi.bravi.exception.ForbiddenException;
import ua.com.bravi.bravi.exception.NotFoundException;
import ua.com.bravi.bravi.exception.StoreAlreadyExistsException;
import ua.com.bravi.bravi.persistance.IStoreEntityRepository;
import ua.com.bravi.bravi.persistance.IUserEntityRepository;
import ua.com.bravi.bravi.persistance.entity.StoreEntity;
import ua.com.bravi.bravi.persistance.entity.UserEntity;
import ua.com.bravi.bravi.persistance.mapper.StoreEntityMapper;

import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    private static final Long SELLER_ID = 42L;

    private final IStoreEntityRepository storeRepository = mock(IStoreEntityRepository.class);
    private final IUserEntityRepository userRepository = mock(IUserEntityRepository.class);
    private final StoreEntityMapper storeEntityMapper = mock(StoreEntityMapper.class);

    private InvocationContext context;
    private StoreService service;

    @BeforeEach
    void setUp() {
        context = new InvocationContext();
        context.setUserId(SELLER_ID);
        context.setUserType(UserType.SELLER);
        service = new StoreService(storeRepository, userRepository, storeEntityMapper, context);
    }

    private static Store newStore() {
        return new Store(
                null, null, "Shop", null, null, null, null,
                null, null, null,
                ZoneId.of("UTC"), null, null,
                Currency.getInstance("UAH"), true,
                null, null, null
        );
    }

    @Test
    void getCurrentUserStoreReturnsMappedDomain() {
        StoreEntity entity = new StoreEntity();
        Store mapped = newStore();
        when(storeRepository.findBySeller_Id(SELLER_ID)).thenReturn(Optional.of(entity));
        when(storeEntityMapper.toDomain(entity)).thenReturn(mapped);

        Store result = service.getCurrentUserStore();

        assertThat(result).isSameAs(mapped);
    }

    @Test
    void getCurrentUserStoreThrowsNotFoundWhenAbsent() {
        when(storeRepository.findBySeller_Id(SELLER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentUserStore())
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Store not found");
    }

    @Test
    void createStorePersistsEntityWithSellerReference() {
        Store store = newStore();
        StoreEntity entity = new StoreEntity();
        UserEntity sellerRef = new UserEntity();

        when(storeRepository.existsBySeller_Id(SELLER_ID)).thenReturn(false);
        when(storeEntityMapper.toEntity(store)).thenReturn(entity);
        when(userRepository.getReferenceById(SELLER_ID)).thenReturn(sellerRef);
        when(storeRepository.save(entity)).thenReturn(entity);

        service.createStore(store);

        assertThat(entity.getSeller()).isSameAs(sellerRef);
        verify(storeRepository).save(entity);
    }

    @Test
    void createStoreRejectsNonSellers() {
        context.setUserType(UserType.BUYER);

        assertThatThrownBy(() -> service.createStore(newStore()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only sellers");

        verify(storeRepository, never()).existsBySeller_Id(any());
        verify(storeRepository, never()).save(any());
    }

    @Test
    void createStoreFailsWhenSellerAlreadyHasStore() {
        when(storeRepository.existsBySeller_Id(SELLER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createStore(newStore()))
                .isInstanceOf(StoreAlreadyExistsException.class);

        verify(storeRepository, never()).save(any());
    }

    @Test
    void createStoreTranslatesUniqueViolationToConflict() {
        Store store = newStore();
        StoreEntity entity = new StoreEntity();

        when(storeRepository.existsBySeller_Id(SELLER_ID)).thenReturn(false);
        when(storeEntityMapper.toEntity(store)).thenReturn(entity);
        when(userRepository.getReferenceById(SELLER_ID)).thenReturn(new UserEntity());
        when(storeRepository.save(entity))
                .thenThrow(new DataIntegrityViolationException("duplicate seller_id"));

        assertThatThrownBy(() -> service.createStore(store))
                .isInstanceOf(StoreAlreadyExistsException.class);
    }

    @Test
    void updateCurrentUserStoreInvokesMapperPatch() {
        Store patch = new Store(
                null, null, "New name", null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null
        );
        StoreEntity entity = new StoreEntity();
        entity.setName("Old");
        entity.setStatus(StoreStatus.ACTIVE);
        when(storeRepository.findBySeller_Id(SELLER_ID)).thenReturn(Optional.of(entity));

        service.updateCurrentUserStore(patch);

        verify(storeEntityMapper).updateEntity(entity, patch);
    }

    @Test
    void updateCurrentUserStoreRejectsNonSellers() {
        context.setUserType(UserType.BUYER);

        assertThatThrownBy(() -> service.updateCurrentUserStore(newStore()))
                .isInstanceOf(ForbiddenException.class);

        verify(storeRepository, never()).findBySeller_Id(any());
    }

    @Test
    void updateCurrentUserStoreThrowsNotFoundWhenStoreMissing() {
        when(storeRepository.findBySeller_Id(SELLER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCurrentUserStore(newStore()))
                .isInstanceOf(NotFoundException.class);

        verify(storeEntityMapper, never()).updateEntity(any(), any());
    }
}
