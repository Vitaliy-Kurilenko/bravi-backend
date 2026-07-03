package ua.com.bravi.bravi.seller.stores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.seller.stores.api.StoreDraft;
import ua.com.bravi.bravi.seller.stores.api.StoreView;
import ua.com.bravi.bravi.seller.stores.api.event.StoreCreatedEvent;
import ua.com.bravi.bravi.seller.stores.domain.Store;
import ua.com.bravi.bravi.seller.stores.domain.StoreStatus;
import ua.com.bravi.bravi.seller.stores.persistence.IStoreEntityRepository;
import ua.com.bravi.bravi.seller.stores.persistence.IStoreSettingsRepository;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreEntity;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreSettingsEntity;
import ua.com.bravi.bravi.seller.stores.persistence.mapper.StoreEntityMapper;

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

    private static final Long ACCOUNT_ID = 42L;
    private static final Long STORE_ID = 7L;

    private final IStoreEntityRepository storeRepository = mock(IStoreEntityRepository.class);
    private final IStoreSettingsRepository storeSettingsRepository = mock(IStoreSettingsRepository.class);
    private final StoreEntityMapper storeEntityMapper = mock(StoreEntityMapper.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private StoreService service;

    @BeforeEach
    void setUp() {
        service = new StoreService(storeRepository, storeSettingsRepository, storeEntityMapper, eventPublisher);
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
    void findFirstStoreIdByAccountIdReturnsId() {
        StoreEntity entity = new StoreEntity();
        entity.setId(STORE_ID);
        when(storeRepository.findFirstBySellerAccountIdOrderByIdAsc(ACCOUNT_ID)).thenReturn(Optional.of(entity));

        assertThat(service.findFirstStoreIdByAccountId(ACCOUNT_ID)).contains(STORE_ID);
    }

    @Test
    void findFirstStoreIdByAccountIdReturnsEmptyWhenAbsent() {
        when(storeRepository.findFirstBySellerAccountIdOrderByIdAsc(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThat(service.findFirstStoreIdByAccountId(ACCOUNT_ID)).isEmpty();
    }

    @Test
    void createDraftStorePersistsDraftWithDefaultsSettingsAndPublishesEvent() {
        StoreDraft draft = new StoreDraft("Shop", "desc", "logo");
        StoreEntity saved = new StoreEntity();
        saved.setId(STORE_ID);
        saved.setSellerAccountId(ACCOUNT_ID);
        StoreView view = mock(StoreView.class);

        when(storeRepository.save(any(StoreEntity.class))).thenReturn(saved);
        when(storeEntityMapper.toView(saved)).thenReturn(view);

        StoreView result = service.createDraftStore(ACCOUNT_ID, draft);

        assertThat(result).isSameAs(view);

        ArgumentCaptor<StoreEntity> captor = ArgumentCaptor.forClass(StoreEntity.class);
        verify(storeRepository).save(captor.capture());
        StoreEntity persisted = captor.getValue();
        assertThat(persisted.getSellerAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(persisted.getName()).isEqualTo("Shop");
        assertThat(persisted.getPublicId()).isNotBlank();
        assertThat(persisted.getStatus()).isEqualTo(StoreStatus.DRAFT);
        assertThat(persisted.getCurrency()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(persisted.getTimezone()).isEqualTo(ZoneId.of("Europe/Lisbon"));

        verify(storeSettingsRepository).save(any(StoreSettingsEntity.class));
        verify(eventPublisher).publishEvent(any(StoreCreatedEvent.class));
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
}
