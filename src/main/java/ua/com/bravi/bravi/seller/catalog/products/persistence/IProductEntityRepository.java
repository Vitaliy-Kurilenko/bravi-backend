package ua.com.bravi.bravi.seller.catalog.products.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductEntity;

import java.util.Optional;

public interface IProductEntityRepository
        extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findByStoreIdAndPublicId(Long storeId, String publicId);
}
