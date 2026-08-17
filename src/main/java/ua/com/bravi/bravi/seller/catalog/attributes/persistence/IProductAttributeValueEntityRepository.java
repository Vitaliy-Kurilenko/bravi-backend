package ua.com.bravi.bravi.seller.catalog.attributes.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.ProductAttributeValueEntity;

import java.util.Collection;
import java.util.List;

public interface IProductAttributeValueEntityRepository extends JpaRepository<ProductAttributeValueEntity, Long> {

    List<ProductAttributeValueEntity> findByProductIdOrderByAttributeIdAscSortOrderAsc(Long productId);

    List<ProductAttributeValueEntity> findByProductIdInOrderByAttributeIdAscSortOrderAsc(Collection<Long> productIds);

    List<ProductAttributeValueEntity> findByProductIdAndAttributeId(Long productId, Long attributeId);

    boolean existsByAttributeId(Long attributeId);

    boolean existsByOptionId(Long optionId);

    void deleteByProductIdAndAttributeIdIn(Long productId, Collection<Long> attributeIds);

    /** Distinct free-text values already entered for an attribute, for suggesting them again. */
    @Query("""
            SELECT DISTINCT v.valueString FROM ProductAttributeValueEntity v
            WHERE v.attributeId = :attributeId
              AND v.valueString IS NOT NULL
              AND LOWER(v.valueString) LIKE :like
            ORDER BY v.valueString
            """)
    List<String> findDistinctValueStrings(@Param("attributeId") Long attributeId, @Param("like") String like,
                                          Pageable pageable);
}
