package ua.com.bravi.bravi.seller.catalog.manufacturers.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.entity.ManufacturerEntity;

import java.util.List;
import java.util.Optional;

public interface IManufacturerEntityRepository
        extends JpaRepository<ManufacturerEntity, Long>, JpaSpecificationExecutor<ManufacturerEntity> {

    List<ManufacturerEntity> findByStoreId(Long storeId);

    Optional<ManufacturerEntity> findByStoreIdAndPublicId(Long storeId, String publicId);
}
