package ua.com.bravi.bravi.seller.catalog.attributes.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.CategoryAttributeEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ICategoryAttributeEntityRepository extends JpaRepository<CategoryAttributeEntity, Long> {

    List<CategoryAttributeEntity> findByCategoryIdInOrderBySortOrderAsc(Collection<Long> categoryIds);

    List<CategoryAttributeEntity> findByCategoryIdOrderBySortOrderAsc(Long categoryId);

    List<CategoryAttributeEntity> findByAttributeId(Long attributeId);

    Optional<CategoryAttributeEntity> findByCategoryIdAndAttributeId(Long categoryId, Long attributeId);

    int countByCategoryId(Long categoryId);
}
