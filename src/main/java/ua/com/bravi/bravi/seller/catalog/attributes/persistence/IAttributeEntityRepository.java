package ua.com.bravi.bravi.seller.catalog.attributes.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeScope;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IAttributeEntityRepository
        extends JpaRepository<AttributeEntity, Long>, JpaSpecificationExecutor<AttributeEntity> {

    Optional<AttributeEntity> findByStoreIdAndPublicId(Long storeId, String publicId);

    List<AttributeEntity> findByStoreIdAndPublicIdIn(Long storeId, Collection<String> publicIds);

    List<AttributeEntity> findByStoreIdAndScope(Long storeId, AttributeScope scope);

    List<AttributeEntity> findByStoreIdAndTemplateCodeIn(Long storeId, Collection<String> templateCodes);

    List<AttributeEntity> findByStoreIdAndIdIn(Long storeId, Collection<Long> ids);
}
