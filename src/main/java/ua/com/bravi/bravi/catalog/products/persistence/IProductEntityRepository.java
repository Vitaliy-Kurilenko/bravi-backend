package ua.com.bravi.bravi.catalog.products.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ua.com.bravi.bravi.catalog.products.persistence.entity.ProductEntity;

public interface IProductEntityRepository
        extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {
}
