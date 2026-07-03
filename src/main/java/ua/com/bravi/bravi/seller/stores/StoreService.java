package ua.com.bravi.bravi.seller.stores;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.shared.exception.ForbiddenException;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.seller.stores.api.StoreView;
import ua.com.bravi.bravi.seller.stores.api.StoresApi;
import ua.com.bravi.bravi.seller.stores.api.event.StoreCreatedEvent;
import ua.com.bravi.bravi.seller.stores.domain.Store;
import ua.com.bravi.bravi.seller.stores.exception.StoreAlreadyExistsException;
import ua.com.bravi.bravi.seller.stores.persistence.IStoreEntityRepository;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreEntity;
import ua.com.bravi.bravi.seller.stores.persistence.mapper.StoreEntityMapper;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StoreService implements StoresApi {

    private final IStoreEntityRepository storeRepository;
    private final StoreEntityMapper storeEntityMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Optional<Long> findStoreIdByUserId(Long userId) {
        return storeRepository.findBySellerId(userId).map(StoreEntity::getId);
    }

    @Override
    public Optional<StoreView> findStoreByUserId(Long userId) {
        return storeRepository.findBySellerId(userId).map(storeEntityMapper::toView);
    }

    @Override
    public Optional<StoreView> getStoreById(Long storeId) {
        return storeRepository.findById(storeId).map(storeEntityMapper::toView);
    }

    @Override
    @Transactional
    public Long createStore(Long sellerId, Store store) {
        if (storeRepository.existsBySellerId(sellerId)) {
            throw new StoreAlreadyExistsException("User already has a store");
        }

        StoreEntity entity = storeEntityMapper.toEntity(store);
        entity.setSellerId(sellerId);

        try {
            StoreEntity saved = storeRepository.save(entity);
            eventPublisher.publishEvent(new StoreCreatedEvent(saved.getId(), sellerId, Instant.now()));
            return saved.getId();
        } catch (DataIntegrityViolationException concurrentInsert) {
            throw new StoreAlreadyExistsException("User already has a store");
        }
    }

    @Override
    @Transactional
    public void updateStore(Long storeId, Store patch) {
        StoreEntity entity = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store not found"));
        storeEntityMapper.updateEntity(entity, patch);
    }

    @Override
    public void requireOwnership(Long storeId, Long userId) {
        Long sellerId = storeRepository.findById(storeId)
                .map(StoreEntity::getSellerId)
                .orElseThrow(() -> new NotFoundException("Store not found"));
        if (!sellerId.equals(userId)) {
            throw new ForbiddenException("Resource does not belong to current user's store");
        }
    }
}
