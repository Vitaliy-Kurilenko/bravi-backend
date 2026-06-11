package ua.com.bravi.bravi.stores.delivery.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.stores.delivery.persistence.entity.StoreDeliveryMethodEntity;

import java.util.List;
import java.util.Optional;

public interface IStoreDeliveryMethodRepository extends JpaRepository<StoreDeliveryMethodEntity, Long> {

    List<StoreDeliveryMethodEntity> findByStoreId(Long storeId);

    List<StoreDeliveryMethodEntity> findByStoreIdAndEnabledTrue(Long storeId);

    Optional<StoreDeliveryMethodEntity> findByStoreIdAndMethodCode(Long storeId, String methodCode);
}
