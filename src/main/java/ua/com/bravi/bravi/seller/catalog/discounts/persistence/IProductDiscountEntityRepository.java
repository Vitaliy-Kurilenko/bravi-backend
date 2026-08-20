package ua.com.bravi.bravi.seller.catalog.discounts.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.com.bravi.bravi.seller.catalog.discounts.persistence.entity.ProductDiscountEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface IProductDiscountEntityRepository extends JpaRepository<ProductDiscountEntity, Long> {

    List<ProductDiscountEntity> findByProductIdOrderByStartsAtAsc(Long productId);

    /**
     * Discounts in effect at {@code at}, for one product or for a whole page. Expressed as a query
     * because the half-open window with a nullable upper bound has no derived-name form.
     */
    @Query("""
           select d from ProductDiscountEntity d
           where d.productId in :productIds
             and d.startsAt <= :at
             and (d.endsAt is null or d.endsAt > :at)
           """)
    List<ProductDiscountEntity> findActiveAt(@Param("productIds") Collection<Long> productIds,
                                             @Param("at") Instant at);
}
