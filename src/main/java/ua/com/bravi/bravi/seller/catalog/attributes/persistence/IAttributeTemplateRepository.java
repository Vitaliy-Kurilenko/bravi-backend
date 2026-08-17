package ua.com.bravi.bravi.seller.catalog.attributes.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeTemplateEntity;

import java.util.Collection;
import java.util.List;

public interface IAttributeTemplateRepository extends JpaRepository<AttributeTemplateEntity, Long> {

    List<AttributeTemplateEntity> findByActiveTrueOrderBySortOrderAscCodeAsc();

    List<AttributeTemplateEntity> findByActiveTrueAndCodeInOrderBySortOrderAscCodeAsc(Collection<String> codes);
}
