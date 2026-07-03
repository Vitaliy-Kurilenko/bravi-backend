package ua.com.bravi.bravi.seller.stores;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.seller.stores.api.StoreView;
import ua.com.bravi.bravi.seller.stores.api.StoresApi;
import ua.com.bravi.bravi.seller.stores.api.event.StoreCreatedEvent;
import ua.com.bravi.bravi.seller.stores.domain.Store;
import ua.com.bravi.bravi.seller.stores.persistence.IStoreEntityRepository;
import ua.com.bravi.bravi.seller.stores.persistence.IStoreSettingsRepository;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreEntity;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreSettingsEntity;
import ua.com.bravi.bravi.seller.stores.persistence.mapper.StoreEntityMapper;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreService implements StoresApi {

    private static final String DEFAULT_WEIGHT_UNIT = "g";
    private static final String DEFAULT_DIMENSION_UNIT = "mm";
    private static final String DEFAULT_LANGUAGE = "uk";

    private final IStoreEntityRepository storeRepository;
    private final IStoreSettingsRepository storeSettingsRepository;
    private final StoreEntityMapper storeEntityMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Optional<Long> findFirstStoreIdByAccountId(Long sellerAccountId) {
        return storeRepository.findFirstBySellerAccountIdOrderByIdAsc(sellerAccountId).map(StoreEntity::getId);
    }

    @Override
    public Optional<StoreView> getStoreById(Long storeId) {
        return storeRepository.findById(storeId).map(storeEntityMapper::toView);
    }

    @Override
    @Transactional
    public Long createStore(Long sellerAccountId, Store store) {
        StoreEntity entity = storeEntityMapper.toEntity(store);
        entity.setPublicId(UUID.randomUUID().toString());
        entity.setSellerAccountId(sellerAccountId);

        StoreEntity saved = storeRepository.save(entity);
        createDefaultSettings(saved);
        eventPublisher.publishEvent(new StoreCreatedEvent(saved.getId(), sellerAccountId, Instant.now()));
        return saved.getId();
    }

    @Override
    @Transactional
    public void updateStore(Long storeId, Store patch) {
        StoreEntity entity = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store not found"));
        storeEntityMapper.updateEntity(entity, patch);
    }

    private void createDefaultSettings(StoreEntity store) {
        StoreSettingsEntity settings = new StoreSettingsEntity();
        settings.setStoreId(store.getId());
        settings.setDefaultWeightUnit(DEFAULT_WEIGHT_UNIT);
        settings.setDefaultDimensionUnit(DEFAULT_DIMENSION_UNIT);
        settings.setDefaultCurrency(store.getCurrency() == null ? null : store.getCurrency().getCurrencyCode());
        settings.setDefaultLanguage(DEFAULT_LANGUAGE);
        settings.setTimezone(store.getTimezone() == null ? null : store.getTimezone().getId());
        storeSettingsRepository.save(settings);
    }
}
