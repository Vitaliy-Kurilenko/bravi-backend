package ua.com.bravi.bravi.catalog.manufacturers.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.catalog.manufacturers.persistence.entity.ManufacturerEntity;

import java.util.List;

public interface IManufacturerEntityRepository extends JpaRepository<ManufacturerEntity, Long> {

    List<ManufacturerEntity> findByStoreId(Long storeId);
}
