package ua.com.bravi.bravi.seller.catalog.manufacturers.api;

import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.Manufacturer;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerSearchQuery;

public interface ManufacturersApi {

    ManufacturerPage search(Long storeId, ManufacturerSearchQuery query);

    /** Internal lookup by bigint id, used by cross-module consumers such as products. */
    ManufacturerView getById(Long storeId, Long manufacturerId);

    ManufacturerView getByPublicId(Long storeId, String publicId);

    ManufacturerView create(Long storeId, Manufacturer manufacturer);

    void update(Long storeId, String publicId, Manufacturer patch);

    void delete(Long storeId, String publicId);
}
