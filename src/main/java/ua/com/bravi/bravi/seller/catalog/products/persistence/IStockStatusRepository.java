package ua.com.bravi.bravi.seller.catalog.products.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.StockStatusEntity;

public interface IStockStatusRepository extends JpaRepository<StockStatusEntity, Long> {
}
