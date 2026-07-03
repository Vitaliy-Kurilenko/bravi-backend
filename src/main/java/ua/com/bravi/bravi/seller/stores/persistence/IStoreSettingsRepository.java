package ua.com.bravi.bravi.seller.stores.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreSettingsEntity;

public interface IStoreSettingsRepository extends JpaRepository<StoreSettingsEntity, Long> {
}
