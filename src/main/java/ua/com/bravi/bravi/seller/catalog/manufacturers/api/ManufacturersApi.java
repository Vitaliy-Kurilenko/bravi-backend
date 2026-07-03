package ua.com.bravi.bravi.seller.catalog.manufacturers.api;

import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.Manufacturer;

import java.util.List;

public interface ManufacturersApi {

    List<ManufacturerView> findByStoreId(Long storeId);

    ManufacturerView getById(Long storeId, Long manufacturerId);

    Long create(Long storeId, Manufacturer manufacturer);

    void update(Long storeId, Long manufacturerId, Manufacturer patch);

    void delete(Long storeId, Long manufacturerId);
}
