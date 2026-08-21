package ua.com.bravi.bravi.seller.tags.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.seller.tags.domain.TagPalette;
import ua.com.bravi.bravi.seller.tags.domain.TagStatus;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;
import ua.com.bravi.bravi.seller.tags.persistence.entity.ProductTagEntity;
import ua.com.bravi.bravi.seller.tags.persistence.entity.TagEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TagEntityRepositoryTest extends AbstractPostgresIT {

    private static final Long IN_STOCK = 1L; // seeded by V9

    @Autowired
    private ITagEntityRepository repository;

    @Autowired
    private IProductTagEntityRepository linkRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void oneStoreAndTargetCannotHoldTwoSpellingsOfOneName() {
        Long storeId = persistStore();
        repository.saveAndFlush(tag(storeId, TagTarget.PRODUCT, "Хіт"));

        assertThatThrownBy(() -> repository.saveAndFlush(tag(storeId, TagTarget.PRODUCT, "хіт")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** Products and orders keep separate vocabularies, so the same word may exist in both. */
    @Test
    void theSameNameIsAllowedForAnotherTarget() {
        Long storeId = persistStore();
        repository.saveAndFlush(tag(storeId, TagTarget.PRODUCT, "Терміново"));

        assertThatCode(() -> repository.saveAndFlush(tag(storeId, TagTarget.ORDER, "Терміново")))
                .doesNotThrowAnyException();
    }

    @Test
    void theSameNameIsAllowedInAnotherStore() {
        Long first = persistStore();
        Long second = persistStore();
        repository.saveAndFlush(tag(first, TagTarget.PRODUCT, "Хіт"));

        assertThatCode(() -> repository.saveAndFlush(tag(second, TagTarget.PRODUCT, "Хіт")))
                .doesNotThrowAnyException();
    }

    /**
     * The auto-create path must never raise: a violation inside the caller's transaction would take
     * the product save down with it.
     */
    @Test
    void insertIfAbsentWritesOnceAndThenQuietlyDoesNothing() {
        Long storeId = persistStore();

        assertThat(insertIfAbsent(storeId, "Хіт")).isEqualTo(1);
        assertThat(insertIfAbsent(storeId, "Хіт")).isZero();
        assertThatCode(() -> insertIfAbsent(storeId, "ХІТ")).doesNotThrowAnyException();
        assertThat(insertIfAbsent(storeId, "ХІТ")).isZero();

        assertThat(repository.findByStoreIdAndTargetOrderByNameAsc(storeId, TagTarget.PRODUCT))
                .extracting(TagEntity::getName)
                .containsExactly("Хіт");
    }

    @Test
    void findsTheMintedRowByItsLowerCasedName() {
        Long storeId = persistStore();
        insertIfAbsent(storeId, "Хіт Сезону");

        assertThat(repository.findByStoreIdAndTargetAndNameKeyIn(storeId, TagTarget.PRODUCT,
                List.of("хіт сезону"))).hasSize(1);
    }

    @Test
    void oneProductCannotCarryTheSameTagTwice() {
        Long storeId = persistStore();
        Long productId = persistProduct(storeId);
        Long tagId = repository.saveAndFlush(tag(storeId, TagTarget.PRODUCT, "Хіт")).getId();
        linkRepository.saveAndFlush(link(productId, tagId));

        assertThatThrownBy(() -> linkRepository.saveAndFlush(link(productId, tagId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingATagUntagsTheProductsItLabelled() {
        Long storeId = persistStore();
        Long productId = persistProduct(storeId);
        TagEntity tag = repository.saveAndFlush(tag(storeId, TagTarget.PRODUCT, "Хіт"));
        linkRepository.saveAndFlush(link(productId, tag.getId()));

        repository.delete(tag);
        repository.flush();
        entityManager.clear();

        assertThat(linkRepository.findByProductIdIn(List.of(productId))).isEmpty();
        assertThat(entityManager.createNativeQuery("SELECT count(*) FROM store_products WHERE id = :id")
                .setParameter("id", productId).getSingleResult())
                .satisfies(count -> assertThat(((Number) count).intValue()).isEqualTo(1));
    }

    @Test
    void deletingAProductTakesItsLinksWithIt() {
        Long storeId = persistStore();
        Long productId = persistProduct(storeId);
        Long tagId = repository.saveAndFlush(tag(storeId, TagTarget.PRODUCT, "Хіт")).getId();
        linkRepository.saveAndFlush(link(productId, tagId));

        entityManager.createNativeQuery("DELETE FROM store_products WHERE id = :id")
                .setParameter("id", productId).executeUpdate();
        entityManager.clear();

        assertThat(linkRepository.findByProductIdIn(List.of(productId))).isEmpty();
        assertThat(repository.findById(tagId)).isPresent();
    }

    /**
     * The database and the colour rules must describe one form. A lower-case value is what
     * normalization exists to prevent, so it is the case worth pinning: were the two to drift, this
     * is where it would show.
     */
    @Test
    void theDatabaseRefusesAColourThatWasNotNormalized() {
        Long storeId = persistStore();
        TagEntity entity = tag(storeId, TagTarget.PRODUCT, "Хіт");
        entity.setColor("#e5484d");

        assertThatThrownBy(() -> repository.saveAndFlush(entity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRefusesAValueThatIsNotAColourAtAll() {
        Long storeId = persistStore();
        TagEntity entity = tag(storeId, TagTarget.PRODUCT, "Хіт");
        entity.setColor("#12345");

        assertThatThrownBy(() -> repository.saveAndFlush(entity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private int insertIfAbsent(Long storeId, String name) {
        return repository.insertIfAbsent(UUID.randomUUID().toString(), storeId,
                TagTarget.PRODUCT.name(), name, TagPalette.pick(name.toLowerCase()),
                TagStatus.ACTIVE.name(), Instant.now());
    }

    private static TagEntity tag(Long storeId, TagTarget target, String name) {
        TagEntity entity = new TagEntity();
        entity.setPublicId(UUID.randomUUID().toString());
        entity.setStoreId(storeId);
        entity.setTarget(target);
        entity.setName(name);
        entity.setColor("#E5484D");
        entity.setStatus(TagStatus.ACTIVE);
        return entity;
    }

    private static ProductTagEntity link(Long productId, Long tagId) {
        ProductTagEntity link = new ProductTagEntity();
        link.setProductId(productId);
        link.setTagId(tagId);
        return link;
    }

    private Long persistStore() {
        Object accountId = entityManager.createNativeQuery(
                        "INSERT INTO accounts (public_id, type, status, created_at) "
                                + "VALUES (:pid, 'SELLER', 'ACTIVE', now()) RETURNING id")
                .setParameter("pid", UUID.randomUUID().toString())
                .getSingleResult();
        long sellerAccountId = ((Number) accountId).longValue();
        entityManager.createNativeQuery(
                        "INSERT INTO seller_accounts (account_id, onboarding_status, created_at) "
                                + "VALUES (:aid, 'ACTIVE', now())")
                .setParameter("aid", sellerAccountId)
                .executeUpdate();
        Object storeId = entityManager.createNativeQuery(
                        "INSERT INTO stores (public_id, seller_account_id, name, status, created_at) "
                                + "VALUES (:spid, :sellerAccountId, 'Shop', 'ACTIVE', now()) RETURNING id")
                .setParameter("spid", UUID.randomUUID().toString())
                .setParameter("sellerAccountId", sellerAccountId)
                .getSingleResult();
        return ((Number) storeId).longValue();
    }

    private Long persistProduct(Long storeId) {
        Object productId = entityManager.createNativeQuery(
                        "INSERT INTO store_products (public_id, store_id, stock_status_id, name, code, "
                                + "price, quantity, status, created_at) "
                                + "VALUES (:ppid, :storeId, :stock, 'Product', :code, 1200, 1, 'ACTIVE', now()) "
                                + "RETURNING id")
                .setParameter("ppid", UUID.randomUUID().toString())
                .setParameter("storeId", storeId)
                .setParameter("stock", IN_STOCK)
                .setParameter("code", UUID.randomUUID().toString())
                .getSingleResult();
        return ((Number) productId).longValue();
    }
}
