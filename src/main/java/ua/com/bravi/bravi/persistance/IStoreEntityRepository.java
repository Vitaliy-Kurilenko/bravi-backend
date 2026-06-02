package ua.com.bravi.bravi.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.persistance.entity.StoreEntity;

import java.util.Optional;

public interface IStoreEntityRepository extends JpaRepository<StoreEntity, Long> {

    Optional<StoreEntity> findBySeller_Id(Long sellerId);

    boolean existsBySeller_Id(Long sellerId);
}
