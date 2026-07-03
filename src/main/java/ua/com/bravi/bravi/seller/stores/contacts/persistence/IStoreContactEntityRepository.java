package ua.com.bravi.bravi.seller.stores.contacts.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.seller.stores.contacts.persistence.entity.StoreContactEntity;

import java.util.List;

public interface IStoreContactEntityRepository extends JpaRepository<StoreContactEntity, Long> {

    List<StoreContactEntity> findByStoreId(Long storeId);

    void deleteByStoreId(Long storeId);
}
