package ua.com.bravi.bravi.seller.catalog.discounts.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountType;
import ua.com.bravi.bravi.seller.catalog.discounts.persistence.entity.ProductDiscountEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductDiscountEntityRepositoryTest extends AbstractPostgresIT {

    private static final Long IN_STOCK = 1L; // seeded by V9

    private static final Instant JAN = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FEB = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant MAR = Instant.parse("2026-03-01T00:00:00Z");
    private static final Instant APR = Instant.parse("2026-04-01T00:00:00Z");

    @Autowired
    private IProductDiscountEntityRepository repository;

    @Autowired
    private EntityManager entityManager;

    private Long persistProduct() {
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
        Object productId = entityManager.createNativeQuery(
                        "INSERT INTO store_products (public_id, store_id, stock_status_id, name, code, " +
                                "price, quantity, status, created_at) " +
                                "VALUES (:ppid, :storeId, :stock, 'Product', :code, 1200, 1, 'ACTIVE', now()) " +
                                "RETURNING id")
                .setParameter("ppid", "prd_" + UUID.randomUUID())
                .setParameter("storeId", ((Number) storeId).longValue())
                .setParameter("stock", IN_STOCK)
                .setParameter("code", UUID.randomUUID().toString())
                .getSingleResult();
        return ((Number) productId).longValue();
    }

    private ProductDiscountEntity discount(Long productId, Instant from, Instant to) {
        return discount(productId, DiscountType.PERCENT, "20", from, to);
    }

    private ProductDiscountEntity discount(Long productId, DiscountType type, String value,
                                           Instant from, Instant to) {
        ProductDiscountEntity entity = new ProductDiscountEntity();
        entity.setPublicId("dsc_" + UUID.randomUUID());
        entity.setProductId(productId);
        entity.setType(type);
        entity.setValue(new BigDecimal(value));
        entity.setStartsAt(from);
        entity.setEndsAt(to);
        return entity;
    }

    @Test
    void rejectsOverlappingPeriodsOfOneProduct() {
        Long productId = persistProduct();
        repository.saveAndFlush(discount(productId, JAN, MAR));

        assertThatThrownBy(() -> repository.saveAndFlush(discount(productId, FEB, APR)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void acceptsPeriodsThatOnlyTouchAtTheBoundary() {
        Long productId = persistProduct();
        repository.saveAndFlush(discount(productId, JAN, FEB));

        assertThatCode(() -> repository.saveAndFlush(discount(productId, FEB, MAR)))
                .doesNotThrowAnyException();
    }

    @Test
    void anOpenEndedDiscountBlocksEveryLaterPeriod() {
        Long productId = persistProduct();
        repository.saveAndFlush(discount(productId, JAN, null));

        assertThatThrownBy(() -> repository.saveAndFlush(discount(productId, MAR, APR)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void anOpenEndedDiscountMayFollowAnEarlierClosedOne() {
        Long productId = persistProduct();
        repository.saveAndFlush(discount(productId, JAN, FEB));

        assertThatCode(() -> repository.saveAndFlush(discount(productId, MAR, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void twoProductsMayHoldTheSamePeriod() {
        Long first = persistProduct();
        Long second = persistProduct();
        repository.saveAndFlush(discount(first, JAN, MAR));

        assertThatCode(() -> repository.saveAndFlush(discount(second, JAN, MAR)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAPercentOutsideTheAllowedRange() {
        Long productId = persistProduct();

        assertThatThrownBy(() -> repository.saveAndFlush(
                discount(productId, DiscountType.PERCENT, "100", JAN, MAR)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsAnAmountOfZero() {
        Long productId = persistProduct();

        assertThatThrownBy(() -> repository.saveAndFlush(
                discount(productId, DiscountType.AMOUNT, "0", JAN, MAR)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsAnEndThatDoesNotFollowTheStart() {
        Long productId = persistProduct();

        assertThatThrownBy(() -> repository.saveAndFlush(discount(productId, MAR, MAR)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsTheDiscountInEffectAtAnInstantWithHalfOpenBounds() {
        Long productId = persistProduct();
        repository.saveAndFlush(discount(productId, FEB, MAR));

        assertThat(repository.findActiveAt(List.of(productId), FEB)).hasSize(1);
        assertThat(repository.findActiveAt(List.of(productId), MAR)).isEmpty();
        assertThat(repository.findActiveAt(List.of(productId), JAN)).isEmpty();
    }

    @Test
    void findsTheDiscountInEffectForAWholePageInOneQuery() {
        Long first = persistProduct();
        Long second = persistProduct();
        Long third = persistProduct();
        repository.saveAndFlush(discount(first, JAN, null));
        repository.saveAndFlush(discount(second, FEB, APR));

        assertThat(repository.findActiveAt(List.of(first, second, third), MAR))
                .extracting(ProductDiscountEntity::getProductId)
                .containsExactlyInAnyOrder(first, second);
    }

    @Test
    void deletingTheProductTakesItsDiscountsWithIt() {
        Long productId = persistProduct();
        repository.saveAndFlush(discount(productId, JAN, MAR));

        entityManager.createNativeQuery("DELETE FROM store_products WHERE id = :id")
                .setParameter("id", productId)
                .executeUpdate();
        entityManager.clear();

        assertThat(repository.findByProductIdOrderByStartsAtAsc(productId)).isEmpty();
    }
}
