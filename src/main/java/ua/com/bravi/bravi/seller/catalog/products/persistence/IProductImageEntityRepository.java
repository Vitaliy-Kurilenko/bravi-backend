package ua.com.bravi.bravi.seller.catalog.products.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductImageEntity;

import java.util.Collection;
import java.util.List;

public interface IProductImageEntityRepository extends JpaRepository<ProductImageEntity, Long> {

    List<ProductImageEntity> findByProductIdOrderBySortOrderAsc(Long productId);

    List<ProductImageEntity> findByProductIdInOrderBySortOrderAsc(Collection<Long> productIds);

    Integer countByProductId(Long productId);
}
