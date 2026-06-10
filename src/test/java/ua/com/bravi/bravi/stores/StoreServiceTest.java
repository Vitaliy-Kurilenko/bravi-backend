package ua.com.bravi.bravi.stores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.shared.exception.ForbiddenException;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.stores.api.event.StoreCreatedEvent;
import ua.com.bravi.bravi.stores.domain.Store;
import ua.com.bravi.bravi.stores.domain.StoreStatus;
import ua.com.bravi.bravi.stores.exception.StoreAlreadyExistsException;
import ua.com.bravi.bravi.stores.persistence.IStoreEntityRepository;
import ua.com.bravi.bravi.stores.persistence.entity.StoreEntity;
import ua.com.bravi.bravi.stores.persistence.mapper.StoreEntityMapper;

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
    private static final Long STORE_ID = 7L;

    private final IStoreEntityRepository storeRepository = mock(IStoreEntityRepository.class);
    private final StoreEntityMapper storeEntityMapper = mock(StoreEntityMapper.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private StoreService service;

    @BeforeEach
    void setUp() {
        service = new StoreService(storeRepository, storeEntityMapper, eventPublisher);
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
    void findStoreIdByUserIdReturnsId() {
        StoreEntity entity = new StoreEntity();
        entity.setId(STORE_ID);
        when(storeRepository.findBySellerId(SELLER_ID)).thenReturn(Optional.of(entity));

        assertThat(service.findStoreIdByUserId(SELLER_ID)).contains(STORE_ID);
    }

    @Test
    void findStoreIdByUserIdReturnsEmptyWhenAbsent() {
        when(storeRepository.findBySellerId(SELLER_ID)).thenReturn(Optional.empty());

        assertThat(service.findStoreIdByUserId(SELLER_ID)).isEmpty();
    }

    @Test
    void createStorePersistsEntityWithSellerIdAndPublishesEvent() {
        Store store = newStore();
        StoreEntity entity = new StoreEntity();
        StoreEntity saved = new StoreEntity();
        saved.setId(STORE_ID);
        saved.setSellerId(SELLER_ID);

        when(storeRepository.existsBySellerId(SELLER_ID)).thenReturn(false);
        when(storeEntityMapper.toEntity(store)).thenReturn(entity);
        when(storeRepository.save(entity)).thenReturn(saved);

        Long resultId = service.createStore(SELLER_ID, store);

        assertThat(resultId).isEqualTo(STORE_ID);
        assertThat(entity.getSellerId()).isEqualTo(SELLER_ID);
        verify(storeRepository).save(entity);
        verify(eventPublisher).publishEvent(any(StoreCreatedEvent.class));
    }

    @Test
    void createStoreFailsWhenSellerAlreadyHasStore() {
        when(storeRepository.existsBySellerId(SELLER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createStore(SELLER_ID, newStore()))
                .isInstanceOf(StoreAlreadyExistsException.class);

        verify(storeRepository, never()).save(any());
    }

    @Test
    void createStoreTranslatesUniqueViolationToConflict() {
        Store store = newStore();
        StoreEntity entity = new StoreEntity();

        when(storeRepository.existsBySellerId(SELLER_ID)).thenReturn(false);
        when(storeEntityMapper.toEntity(store)).thenReturn(entity);
        when(storeRepository.save(entity))
                .thenThrow(new DataIntegrityViolationException("duplicate seller_id"));

        assertThatThrownBy(() -> service.createStore(SELLER_ID, store))
                .isInstanceOf(StoreAlreadyExistsException.class);
    }

    @Test
    void updateStoreInvokesMapperPatch() {
        Store patch = new Store(
                null, null, "New name", null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null
        );
        StoreEntity entity = new StoreEntity();
        entity.setName("Old");
        entity.setStatus(StoreStatus.ACTIVE);
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(entity));

        service.updateStore(STORE_ID, patch);

        verify(storeEntityMapper).updateEntity(entity, patch);
    }

    @Test
    void updateStoreThrowsNotFoundWhenStoreMissing() {
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStore(STORE_ID, newStore()))
                .isInstanceOf(NotFoundException.class);

        verify(storeEntityMapper, never()).updateEntity(any(), any());
    }

    @Test
    void requireOwnershipPassesWhenSellerMatches() {
        StoreEntity entity = new StoreEntity();
        entity.setSellerId(SELLER_ID);
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(entity));

        service.requireOwnership(STORE_ID, SELLER_ID);
    }

    @Test
    void requireOwnershipThrowsForbiddenWhenSellerMismatches() {
        StoreEntity entity = new StoreEntity();
        entity.setSellerId(SELLER_ID);
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.requireOwnership(STORE_ID, 999L))
                .isInstanceOf(ForbiddenException.class);
    }
}
