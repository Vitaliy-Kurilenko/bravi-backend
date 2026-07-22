package ua.com.bravi.bravi.seller.catalog.categories.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.seller.catalog.categories.persistence.entity.CategoryEntity;

import java.util.List;
import java.util.Optional;

public interface ICategoryEntityRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findByStoreId(Long storeId);

    Optional<CategoryEntity> findByStoreIdAndPublicId(Long storeId, String publicId);

    boolean existsByParentId(Long parentId);
}
