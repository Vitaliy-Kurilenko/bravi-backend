package ua.com.bravi.bravi.seller.catalog.attributes.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeTemplateOptionEntity;

import java.util.Collection;
import java.util.List;

public interface IAttributeTemplateOptionRepository extends JpaRepository<AttributeTemplateOptionEntity, Long> {

    List<AttributeTemplateOptionEntity> findByTemplateIdInOrderBySortOrderAsc(Collection<Long> templateIds);
}
