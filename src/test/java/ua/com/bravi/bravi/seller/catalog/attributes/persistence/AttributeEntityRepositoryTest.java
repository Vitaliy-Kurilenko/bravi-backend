package ua.com.bravi.bravi.seller.catalog.attributes.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeScope;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeStatus;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValueType;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeEntity;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeOptionEntity;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.ProductAttributeValueEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AttributeEntityRepositoryTest extends AbstractPostgresIT {

    private static final Long IN_STOCK = 1L; // seeded by V7

    @Autowired
    private IAttributeEntityRepository attributeRepository;

    @Autowired
    private IAttributeOptionEntityRepository optionRepository;

    @Autowired
    private IProductAttributeValueEntityRepository valueRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void enforcesUniqueCodePerStore() {
        Long storeId = persistStore();
        attributeRepository.saveAndFlush(attribute(storeId, "COLOR", AttributeValueType.SELECT));

        assertThatThrownBy(() -> attributeRepository.saveAndFlush(
                attribute(storeId, "COLOR", AttributeValueType.TEXT)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theSameCodeMayExistInAnotherStore() {
        AttributeEntity first = attributeRepository.saveAndFlush(
                attribute(persistStore(), "COLOR", AttributeValueType.SELECT));
        AttributeEntity second = attributeRepository.saveAndFlush(
                attribute(persistStore(), "COLOR", AttributeValueType.SELECT));

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void enforcesUniqueOptionCodePerAttribute() {
        AttributeEntity attribute = attributeRepository.saveAndFlush(
                attribute(persistStore(), "COLOR", AttributeValueType.SELECT));
        optionRepository.saveAndFlush(option(attribute, "RED", 0));

        assertThatThrownBy(() -> optionRepository.saveAndFlush(option(attribute, "RED", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void multiSelectKeepsOneRowPerChosenOptionButOnlyOneRowPerScalar() {
        Long storeId = persistStore();
        AttributeEntity material = attributeRepository.saveAndFlush(
                attribute(storeId, "MATERIAL", AttributeValueType.MULTI_SELECT));
        AttributeOptionEntity cotton = optionRepository.saveAndFlush(option(material, "COTTON", 0));
        AttributeOptionEntity wool = optionRepository.saveAndFlush(option(material, "WOOL", 1));
        Long productId = persistProduct(storeId);

        valueRepository.saveAndFlush(optionValue(productId, material, cotton, 0));
        valueRepository.saveAndFlush(optionValue(productId, material, wool, 1));
        assertThat(valueRepository.findByProductIdAndAttributeId(productId, material.getId())).hasSize(2);

        AttributeEntity weight = attributeRepository.saveAndFlush(
                attribute(storeId, "WEIGHT", AttributeValueType.NUMBER));
        valueRepository.saveAndFlush(numberValue(productId, weight, "1.5"));

        assertThatThrownBy(() -> valueRepository.saveAndFlush(numberValue(productId, weight, "2.5")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAnAttributeCascadesToItsOptions() {
        AttributeEntity attribute = attributeRepository.saveAndFlush(
                attribute(persistStore(), "COLOR", AttributeValueType.SELECT));
        optionRepository.saveAndFlush(option(attribute, "RED", 0));

        attributeRepository.delete(attribute);
        attributeRepository.flush();
        entityManager.clear();

        assertThat(optionRepository.findByAttributeIdOrderBySortOrderAsc(attribute.getId())).isEmpty();
    }

    @Test
    void anAttributeCarryingProductValuesCannotBeDeleted() {
        Long storeId = persistStore();
        AttributeEntity attribute = attributeRepository.saveAndFlush(
                attribute(storeId, "WEIGHT", AttributeValueType.NUMBER));
        valueRepository.saveAndFlush(numberValue(persistProduct(storeId), attribute, "1.5"));

        assertThat(valueRepository.existsByAttributeId(attribute.getId())).isTrue();
        assertThatThrownBy(() -> {
            attributeRepository.delete(attribute);
            attributeRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsGlobalAttributesOfTheStoreOnly() {
        Long storeId = persistStore();
        AttributeEntity global = attribute(storeId, "WARRANTY", AttributeValueType.NUMBER);
        global.setScope(AttributeScope.GLOBAL);
        attributeRepository.saveAndFlush(global);
        attributeRepository.saveAndFlush(attribute(storeId, "COLOR", AttributeValueType.SELECT));

        AttributeEntity otherStoreGlobal = attribute(persistStore(), "WARRANTY", AttributeValueType.NUMBER);
        otherStoreGlobal.setScope(AttributeScope.GLOBAL);
        attributeRepository.saveAndFlush(otherStoreGlobal);

        assertThat(attributeRepository.findByStoreIdAndScope(storeId, AttributeScope.GLOBAL))
                .extracting(AttributeEntity::getCode)
                .containsExactly("WARRANTY");
    }

    @Test
    void suggestsDistinctFreeTextValuesOfOneAttribute() {
        Long storeId = persistStore();
        AttributeEntity contents = attributeRepository.saveAndFlush(
                attribute(storeId, "CONTENTS", AttributeValueType.TEXT));
        valueRepository.saveAndFlush(textValue(persistProduct(storeId), contents, "Charger and cable"));
        valueRepository.saveAndFlush(textValue(persistProduct(storeId), contents, "Charger and cable"));
        valueRepository.saveAndFlush(textValue(persistProduct(storeId), contents, "Case only"));

        List<String> suggestions = valueRepository.findDistinctValueStrings(
                contents.getId(), "%charger%", org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(suggestions).containsExactly("Charger and cable");
    }

    private Long persistStore() {
        Object accountId = entityManager.createNativeQuery(
                        "INSERT INTO accounts (public_id, type, status, created_at) " +
                                "VALUES (:pid, 'SELLER', 'ACTIVE', now()) RETURNING id")
                .setParameter("pid", UUID.randomUUID().toString())
                .getSingleResult();
        long sellerAccountId = ((Number) accountId).longValue();
        entityManager.createNativeQuery(
                        "INSERT INTO seller_accounts (account_id, onboarding_status, created_at) " +
                                "VALUES (:aid, 'ACTIVE', now())")
                .setParameter("aid", sellerAccountId)
                .executeUpdate();
        Object storeId = entityManager.createNativeQuery(
                        "INSERT INTO stores (public_id, seller_account_id, name, status, created_at) " +
                                "VALUES (:spid, :sellerAccountId, 'Shop', 'ACTIVE', now()) RETURNING id")
                .setParameter("spid", UUID.randomUUID().toString())
                .setParameter("sellerAccountId", sellerAccountId)
                .getSingleResult();
        return ((Number) storeId).longValue();
    }

    private Long persistProduct(Long storeId) {
        Object productId = entityManager.createNativeQuery(
                        "INSERT INTO store_products (public_id, store_id, stock_status_id, name, code, price, " +
                                "quantity, status, created_at) " +
                                "VALUES (:pid, :storeId, :stockStatusId, 'Widget', :code, 10, 1, 'ACTIVE', now()) " +
                                "RETURNING id")
                .setParameter("pid", "prd_" + UUID.randomUUID())
                .setParameter("storeId", storeId)
                .setParameter("stockStatusId", IN_STOCK)
                .setParameter("code", UUID.randomUUID().toString())
                .getSingleResult();
        return ((Number) productId).longValue();
    }

    private static AttributeEntity attribute(Long storeId, String code, AttributeValueType valueType) {
        AttributeEntity entity = new AttributeEntity();
        entity.setPublicId("attr_" + UUID.randomUUID());
        entity.setStoreId(storeId);
        entity.setCode(code);
        entity.setName(code);
        entity.setValueType(valueType);
        entity.setScope(AttributeScope.CATEGORY);
        entity.setStatus(AttributeStatus.ACTIVE);
        return entity;
    }

    private static AttributeOptionEntity option(AttributeEntity attribute, String code, int sortOrder) {
        AttributeOptionEntity entity = new AttributeOptionEntity();
        entity.setPublicId("aopt_" + UUID.randomUUID());
        entity.setAttributeId(attribute.getId());
        entity.setCode(code);
        entity.setName(code);
        entity.setSortOrder(sortOrder);
        return entity;
    }

    private static ProductAttributeValueEntity optionValue(Long productId, AttributeEntity attribute,
                                                           AttributeOptionEntity option, int sortOrder) {
        ProductAttributeValueEntity entity = new ProductAttributeValueEntity();
        entity.setProductId(productId);
        entity.setAttributeId(attribute.getId());
        entity.setOptionId(option.getId());
        entity.setSortOrder(sortOrder);
        return entity;
    }

    private static ProductAttributeValueEntity numberValue(Long productId, AttributeEntity attribute, String value) {
        ProductAttributeValueEntity entity = new ProductAttributeValueEntity();
        entity.setProductId(productId);
        entity.setAttributeId(attribute.getId());
        entity.setValueNumber(new BigDecimal(value));
        return entity;
    }

    private static ProductAttributeValueEntity textValue(Long productId, AttributeEntity attribute, String value) {
        ProductAttributeValueEntity entity = new ProductAttributeValueEntity();
        entity.setProductId(productId);
        entity.setAttributeId(attribute.getId());
        entity.setValueString(value);
        return entity;
    }
}
