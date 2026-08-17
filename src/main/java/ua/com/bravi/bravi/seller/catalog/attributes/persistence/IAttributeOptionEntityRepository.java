package ua.com.bravi.bravi.seller.catalog.attributes.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeOptionEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IAttributeOptionEntityRepository extends JpaRepository<AttributeOptionEntity, Long> {

    List<AttributeOptionEntity> findByAttributeIdOrderBySortOrderAsc(Long attributeId);

    List<AttributeOptionEntity> findByAttributeIdInOrderBySortOrderAsc(Collection<Long> attributeIds);

    Optional<AttributeOptionEntity> findByAttributeIdAndPublicId(Long attributeId, String publicId);

    int countByAttributeId(Long attributeId);
}
