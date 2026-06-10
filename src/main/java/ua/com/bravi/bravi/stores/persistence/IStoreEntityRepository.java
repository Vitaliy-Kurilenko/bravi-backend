package ua.com.bravi.bravi.stores.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.stores.persistence.entity.StoreEntity;

import java.util.Optional;

public interface IStoreEntityRepository extends JpaRepository<StoreEntity, Long> {

    Optional<StoreEntity> findBySellerId(Long sellerId);

    boolean existsBySellerId(Long sellerId);
}
