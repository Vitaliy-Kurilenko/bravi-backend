package ua.com.bravi.bravi.seller.catalog.discounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountBulkResultView;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountTarget;
import ua.com.bravi.bravi.seller.catalog.discounts.api.ProductDiscountView;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.Discount;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountType;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.SkipReason;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.SubmittedDiscount;
import ua.com.bravi.bravi.seller.catalog.discounts.exception.DiscountOverlapException;
import ua.com.bravi.bravi.seller.catalog.discounts.exception.InvalidDiscountRequestException;
import ua.com.bravi.bravi.seller.catalog.discounts.persistence.IProductDiscountEntityRepository;
import ua.com.bravi.bravi.seller.catalog.discounts.persistence.entity.ProductDiscountEntity;
import ua.com.bravi.bravi.seller.catalog.discounts.persistence.mapper.ProductDiscountEntityMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiscountServiceTest {

    private static final Long STORE_ID = 7L;
    private static final Long PRODUCT_ID = 42L;
    private static final BigDecimal PRICE = new BigDecimal("1200.0000");
    private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant CREATED = Instant.parse("2025-12-01T00:00:00Z");

    private final IProductDiscountEntityRepository repository = mock(IProductDiscountEntityRepository.class);
    private final ProductDiscountEntityMapper mapper = mock(ProductDiscountEntityMapper.class);

    private DiscountService service;

    @BeforeEach
    void setUp() {
        service = new DiscountService(repository, mapper);
    }

    private static Instant plusDays(long days) {
        return NOW.plus(days, ChronoUnit.DAYS);
    }

    private static ProductDiscountEntity entity(Long id, String publicId, Instant from, Instant to) {
        ProductDiscountEntity stored = new ProductDiscountEntity();
        stored.setId(id);
        stored.setPublicId(publicId);
        stored.setProductId(PRODUCT_ID);
        stored.setType(DiscountType.PERCENT);
        stored.setValue(new BigDecimal("20"));
        stored.setStartsAt(from);
        stored.setEndsAt(to);
        stored.setCreatedAt(CREATED);
        return stored;
    }

    private static Discount domain(Long id, String publicId, Instant from, Instant to) {
        return new Discount(id, publicId, PRODUCT_ID, DiscountType.PERCENT, new BigDecimal("20"),
                from, to, null, id == null ? null : CREATED, null);
    }

    private static SubmittedDiscount submitted(int index, String publicId, Instant from, Instant to) {
        return new SubmittedDiscount(index, new Discount(null, publicId, null, DiscountType.PERCENT,
                new BigDecimal("20"), from, to, null, null, null));
    }

    @Test
    void replacingASetEmptiesTheProductSliceBeforeWritingTheNewOne() {
        // Any legal reshuffle of periods would trip the exclusion constraint if inserts landed first.
        ProductDiscountEntity stored = entity(1L, "dsc_a", plusDays(10), plusDays(20));
        when(repository.findByProductIdOrderByStartsAtAsc(PRODUCT_ID)).thenReturn(List.of(stored));
        when(mapper.toDomains(List.of(stored))).thenReturn(List.of(domain(1L, "dsc_a", plusDays(10), plusDays(20))));
        when(mapper.toEntity(any())).thenAnswer(invocation -> new ProductDiscountEntity());

        service.replaceForProduct(STORE_ID, PRODUCT_ID, PRICE,
                List.of(submitted(0, "dsc_a", plusDays(30), plusDays(40))), NOW);

        InOrder order = inOrder(repository);
        order.verify(repository).deleteAllInBatch(List.of(stored));
        order.verify(repository).flush();
        order.verify(repository).saveAll(anyList());
        order.verify(repository).flush();
    }

    @Test
    void aResubmittedDiscountKeepsItsPublicIdAndCreationTimeWhileANewOneGetsAPrefixedId() {
        ProductDiscountEntity stored = entity(1L, "dsc_keep", plusDays(10), plusDays(20));
        when(repository.findByProductIdOrderByStartsAtAsc(PRODUCT_ID)).thenReturn(List.of(stored));
        when(mapper.toDomains(List.of(stored)))
                .thenReturn(List.of(domain(1L, "dsc_keep", plusDays(10), plusDays(20))));
        when(mapper.toEntity(any())).thenAnswer(invocation -> new ProductDiscountEntity());

        service.replaceForProduct(STORE_ID, PRODUCT_ID, PRICE, List.of(
                submitted(0, "dsc_keep", plusDays(10), plusDays(20)),
                submitted(1, null, plusDays(30), plusDays(40))), NOW);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductDiscountEntity>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());

        assertThat(saved.getValue()).hasSize(2);
        assertThat(saved.getValue().getFirst().getPublicId()).isEqualTo("dsc_keep");
        assertThat(saved.getValue().getFirst().getCreatedAt()).isEqualTo(CREATED);
        assertThat(saved.getValue().get(1).getPublicId()).startsWith("dsc_");
        assertThat(saved.getValue().get(1).getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void anOverlapInTheSubmittedSetIsRejectedBeforeAnythingIsWritten() {
        when(repository.findByProductIdOrderByStartsAtAsc(PRODUCT_ID)).thenReturn(List.of());
        when(mapper.toDomains(List.of())).thenReturn(List.of());

        assertThatThrownBy(() -> service.replaceForProduct(STORE_ID, PRODUCT_ID, PRICE, List.of(
                submitted(0, null, plusDays(10), plusDays(30)),
                submitted(1, null, plusDays(20), plusDays(40))), NOW))
                .isInstanceOf(DiscountOverlapException.class);

        verify(repository, never()).deleteAllInBatch(anyList());
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void aConstraintViolationBecomesAConcurrentChangeConflict() {
        when(repository.findByProductIdOrderByStartsAtAsc(PRODUCT_ID)).thenReturn(List.of());
        when(mapper.toDomains(List.of())).thenReturn(List.of());
        when(mapper.toEntity(any())).thenAnswer(invocation -> new ProductDiscountEntity());
        when(repository.saveAll(anyList())).thenThrow(new DataIntegrityViolationException("conflicting key"));

        assertThatThrownBy(() -> service.replaceForProduct(STORE_ID, PRODUCT_ID, PRICE,
                List.of(submitted(0, null, plusDays(10), plusDays(20))), NOW))
                .isInstanceOfSatisfying(DiscountOverlapException.class,
                        ex -> assertThat(ex.getConflicting()).isNull());
    }

    @Test
    void pricingAWholePageTakesASingleQuery() {
        List<DiscountTarget> targets = List.of(
                new DiscountTarget(1L, "prd_1", new BigDecimal("1200")),
                new DiscountTarget(2L, "prd_2", new BigDecimal("500")),
                new DiscountTarget(3L, "prd_3", new BigDecimal("100")));
        ProductDiscountEntity active = entity(9L, "dsc_live", NOW.minus(1, ChronoUnit.DAYS), plusDays(1));
        active.setProductId(1L);
        when(repository.findActiveAt(any(), any())).thenReturn(List.of(active));
        when(mapper.toDomain(active)).thenReturn(new Discount(9L, "dsc_live", 1L, DiscountType.PERCENT,
                new BigDecimal("20"), NOW.minus(1, ChronoUnit.DAYS), plusDays(1), null, CREATED, null));

        Map<Long, ProductDiscountView> priced = service.activeByProduct(STORE_ID, targets, NOW);

        verify(repository, times(1)).findActiveAt(any(), any());
        assertThat(priced).containsOnlyKeys(1L);
        assertThat(priced.get(1L).discountedPrice()).isEqualByComparingTo("960.00");
    }

    @Test
    void anEmptyPageNeverTouchesTheDatabase() {
        assertThat(service.activeByProduct(STORE_ID, List.of(), NOW)).isEmpty();

        verify(repository, never()).findActiveAt(any(), any());
    }

    @Test
    void bulkSkipsProductsItCannotDiscountAndCountsTheRest() {
        ProductDiscountEntity taken = entity(1L, "dsc_taken", plusDays(10), plusDays(20));
        taken.setProductId(2L);
        when(repository.findByProductIdOrderByStartsAtAsc(1L)).thenReturn(List.of());
        when(mapper.toDomains(List.of())).thenReturn(List.of());
        when(repository.findByProductIdOrderByStartsAtAsc(2L)).thenReturn(List.of(taken));
        when(mapper.toDomains(List.of(taken)))
                .thenReturn(List.of(new Discount(1L, "dsc_taken", 2L, DiscountType.PERCENT, new BigDecimal("20"),
                        plusDays(10), plusDays(20), null, CREATED, null)));
        when(repository.findByProductIdOrderByStartsAtAsc(3L)).thenReturn(List.of());
        when(mapper.toEntity(any())).thenAnswer(invocation -> new ProductDiscountEntity());

        Discount candidate = new Discount(null, null, null, DiscountType.AMOUNT, new BigDecimal("100"),
                plusDays(12), plusDays(18), null, null, null);

        DiscountBulkResultView result = service.applyBulk(STORE_ID, List.of(
                new DiscountTarget(1L, "prd_1", new BigDecimal("1200")),
                new DiscountTarget(2L, "prd_2", new BigDecimal("1200")),
                new DiscountTarget(3L, "prd_3", new BigDecimal("50"))), candidate, NOW);

        assertThat(result.applied()).isEqualTo(1);
        assertThat(result.skipped()).hasSize(2)
                .anySatisfy(skipped -> {
                    assertThat(skipped.productPublicId()).isEqualTo("prd_2");
                    assertThat(skipped.reason()).isEqualTo(SkipReason.PERIOD_OVERLAP);
                    assertThat(skipped.conflictingDiscountPublicId()).isEqualTo("dsc_taken");
                })
                .anySatisfy(skipped -> {
                    assertThat(skipped.productPublicId()).isEqualTo("prd_3");
                    assertThat(skipped.reason()).isEqualTo(SkipReason.AMOUNT_EXCEEDS_PRICE);
                });
    }

    @Test
    void aPriceChangeIsRejectedWhileALiveAmountDiscountWouldNotStayBelowIt() {
        ProductDiscountEntity live = entity(1L, "dsc_amount", NOW.minus(1, ChronoUnit.DAYS), plusDays(1));
        when(repository.findByProductIdOrderByStartsAtAsc(PRODUCT_ID)).thenReturn(List.of(live));
        when(mapper.toDomains(List.of(live))).thenReturn(List.of(new Discount(1L, "dsc_amount", PRODUCT_ID,
                DiscountType.AMOUNT, new BigDecimal("500"), NOW.minus(1, ChronoUnit.DAYS), plusDays(1),
                null, CREATED, null)));

        assertThatThrownBy(() -> service.requireCompatibleWithPrice(STORE_ID, PRODUCT_ID,
                new BigDecimal("400"), NOW))
                .isInstanceOfSatisfying(InvalidDiscountRequestException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("price"));
    }
}
