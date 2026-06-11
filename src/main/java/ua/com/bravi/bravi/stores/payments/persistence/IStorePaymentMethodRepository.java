package ua.com.bravi.bravi.stores.payments.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.stores.payments.persistence.entity.StorePaymentMethodEntity;

import java.util.List;
import java.util.Optional;

public interface IStorePaymentMethodRepository extends JpaRepository<StorePaymentMethodEntity, Long> {

    List<StorePaymentMethodEntity> findByStoreId(Long storeId);

    List<StorePaymentMethodEntity> findByStoreIdAndEnabledTrue(Long storeId);

    Optional<StorePaymentMethodEntity> findByStoreIdAndMethodCode(Long storeId, String methodCode);
}
