package ua.com.bravi.bravi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.component.InvocationContext;
import ua.com.bravi.bravi.domain.store.Store;
import ua.com.bravi.bravi.domain.user.UserType;
import ua.com.bravi.bravi.exception.ForbiddenException;
import ua.com.bravi.bravi.exception.NotFoundException;
import ua.com.bravi.bravi.exception.StoreAlreadyExistsException;
import ua.com.bravi.bravi.persistance.IStoreEntityRepository;
import ua.com.bravi.bravi.persistance.IUserEntityRepository;
import ua.com.bravi.bravi.persistance.entity.StoreEntity;
import ua.com.bravi.bravi.persistance.mapper.StoreEntityMapper;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final IStoreEntityRepository storeRepository;
    private final IUserEntityRepository userRepository;
    private final StoreEntityMapper storeEntityMapper;
    private final InvocationContext invocationContext;

    public Store getCurrentUserStore() {
        return storeRepository.findBySeller_Id(invocationContext.getUserId())
                .map(storeEntityMapper::toDomain)
                .orElseThrow(() -> new NotFoundException("Store not found"));
    }

    @Transactional
    public void createStore(Store store) {
        requireSeller();
        Long sellerId = invocationContext.getUserId();

        if (storeRepository.existsBySeller_Id(sellerId)) {
            throw new StoreAlreadyExistsException("User already has a store");
        }

        StoreEntity entity = storeEntityMapper.toEntity(store);
        entity.setSeller(userRepository.getReferenceById(sellerId));

        try {
            storeRepository.save(entity);
        } catch (DataIntegrityViolationException concurrentInsert) {
            throw new StoreAlreadyExistsException("User already has a store");
        }
    }

    @Transactional
    public void updateCurrentUserStore(Store patch) {
        requireSeller();

        StoreEntity entity = storeRepository.findBySeller_Id(invocationContext.getUserId())
                .orElseThrow(() -> new NotFoundException("Store not found"));

        storeEntityMapper.updateEntity(entity, patch);
    }

    private void requireSeller() {
        if (invocationContext.getUserType() != UserType.SELLER) {
            throw new ForbiddenException("Only sellers can manage stores");
        }
    }
}
