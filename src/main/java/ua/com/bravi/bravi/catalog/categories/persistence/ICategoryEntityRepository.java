package ua.com.bravi.bravi.catalog.categories.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.catalog.categories.persistence.entity.CategoryEntity;

import java.util.List;

public interface ICategoryEntityRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findByStoreId(Long storeId);

    boolean existsByParentId(Long parentId);
}
