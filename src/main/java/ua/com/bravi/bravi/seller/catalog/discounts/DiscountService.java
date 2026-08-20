package ua.com.bravi.bravi.seller.catalog.discounts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountBulkResultView;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountTarget;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountView;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountsApi;
import ua.com.bravi.bravi.seller.catalog.discounts.api.ProductDiscountView;
import ua.com.bravi.bravi.seller.catalog.discounts.api.SkippedProductView;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.Discount;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountPolicy;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountPricing;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountType;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountReplacement;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.DiscountSchedule;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.SkipReason;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.SubmittedDiscount;
import ua.com.bravi.bravi.seller.catalog.discounts.exception.DiscountOverlapException;
import ua.com.bravi.bravi.seller.catalog.discounts.persistence.IProductDiscountEntityRepository;
import ua.com.bravi.bravi.seller.catalog.discounts.persistence.entity.ProductDiscountEntity;
import ua.com.bravi.bravi.seller.catalog.discounts.persistence.mapper.ProductDiscountEntityMapper;
import ua.com.bravi.bravi.shared.util.PublicIdGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Owns the discount schedules of products. Discounts live here rather than in the product aggregate
 * because validating one needs the whole schedule and interval arithmetic the product does not hold;
 * products reach this service through {@code DiscountsApi}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService implements DiscountsApi {

    private static final String CONCURRENT_CHANGE =
            "Another change to this product's discounts overlapped yours; reload and try again";

    private final IProductDiscountEntityRepository discountRepository;
    private final ProductDiscountEntityMapper discountEntityMapper;

    @Override
    public List<DiscountView> listForProduct(Long storeId, Long productId, Instant at) {
        return toViews(discountEntityMapper.toDomains(
                discountRepository.findByProductIdOrderByStartsAtAsc(productId)), at);
    }

    @Override
    @Transactional
    public List<DiscountView> replaceForProduct(Long storeId, Long productId, BigDecimal productPrice,
                                                List<SubmittedDiscount> submitted, Instant at) {
        List<ProductDiscountEntity> stored = discountRepository.findByProductIdOrderByStartsAtAsc(productId);
        DiscountReplacement plan = DiscountReplacement.plan(
                discountEntityMapper.toDomains(stored), submitted, at);
        validate(plan, storeId, productId, productPrice, at);

        // Every row is rewritten rather than patched in place: emptying the product's slice first means
        // a legal reshuffle of periods never passes through a state the exclusion constraint rejects,
        // which patching cannot guarantee since JPA flushes inserts before deletes.
        discountRepository.deleteAllInBatch(stored);
        discountRepository.flush();

        List<ProductDiscountEntity> rewritten = plan.resulting().stream()
                .map(entry -> toEntity(entry.discount(), productId, at))
                .toList();
        try {
            discountRepository.saveAll(rewritten);
            discountRepository.flush();
        } catch (DataIntegrityViolationException violation) {
            throw concurrentChange(violation, storeId, productId);
        }

        log.info("Product discounts replaced storeId={} productId={} kept={} created={} deleted={}",
                storeId, productId, plan.updated().size(), plan.created().size(), plan.deleted().size());
        return listForProduct(storeId, productId, at);
    }

    @Override
    public Optional<ProductDiscountView> activeForProduct(Long storeId, Long productId,
                                                          BigDecimal price, Instant at) {
        return discountRepository.findActiveAt(List.of(productId), at).stream()
                .findFirst()
                .map(entity -> toProductDiscount(discountEntityMapper.toDomain(entity), price, at));
    }

    @Override
    public Map<Long, ProductDiscountView> activeByProduct(Long storeId, List<DiscountTarget> targets, Instant at) {
        if (targets.isEmpty()) {
            return Map.of();
        }
        Map<Long, BigDecimal> priceByProduct = targets.stream()
                .collect(Collectors.toMap(DiscountTarget::productId, DiscountTarget::price, (first, ignored) -> first));

        Map<Long, ProductDiscountView> active = new LinkedHashMap<>();
        for (ProductDiscountEntity entity : discountRepository.findActiveAt(priceByProduct.keySet(), at)) {
            BigDecimal price = priceByProduct.get(entity.getProductId());
            active.put(entity.getProductId(),
                    toProductDiscount(discountEntityMapper.toDomain(entity), price, at));
        }
        return active;
    }

    @Override
    @Transactional
    public DiscountBulkResultView applyBulk(Long storeId, List<DiscountTarget> targets,
                                            Discount discount, Instant at) {
        // A flat request, so errors name the properties the client sent rather than array positions.
        // The per-product rules (an occupied period, a price a fixed sum would not stay below) are not
        // failures of the request and are reported as skipped products instead.
        DiscountPolicy.validatePeriod("ends_at", discount.period());
        DiscountPolicy.requireNotEntirelyPast("ends_at", discount.period(), at);
        if (discount.type() == DiscountType.PERCENT) {
            DiscountPolicy.validateValue("value", discount.type(), discount.value(), null);
        }

        List<SkippedProductView> skipped = new ArrayList<>();
        List<ProductDiscountEntity> created = new ArrayList<>();

        for (DiscountTarget target : targets) {
            DiscountSchedule schedule = DiscountSchedule.of(discountEntityMapper.toDomains(
                    discountRepository.findByProductIdOrderByStartsAtAsc(target.productId())));
            Optional<SkipReason> reason = DiscountPolicy.checkAddition(schedule, discount, target.price(), at);
            if (reason.isPresent()) {
                skipped.add(new SkippedProductView(target.productPublicId(), reason.get(),
                        schedule.conflictWith(discount.period(), null).map(Discount::publicId).orElse(null)));
                continue;
            }
            created.add(toEntity(discount, target.productId(), at));
        }

        try {
            discountRepository.saveAll(created);
            discountRepository.flush();
        } catch (DataIntegrityViolationException violation) {
            throw concurrentChange(violation, storeId, null);
        }

        log.info("Discounts applied in bulk storeId={} products={} applied={} skipped={}",
                storeId, targets.size(), created.size(), skipped.size());
        return new DiscountBulkResultView(created.size(), List.copyOf(skipped));
    }

    @Override
    public void requireCompatibleWithPrice(Long storeId, Long productId, BigDecimal newPrice, Instant at) {
        DiscountSchedule schedule = DiscountSchedule.of(discountEntityMapper.toDomains(
                discountRepository.findByProductIdOrderByStartsAtAsc(productId)));
        try {
            DiscountPolicy.requireCompatibleWithPrice(schedule, newPrice, at);
        } catch (RuntimeException rejected) {
            log.warn("Product price change rejected storeId={} productId={} reason=amount_discount", storeId, productId);
            throw rejected;
        }
    }

    private void validate(DiscountReplacement plan, Long storeId, Long productId,
                          BigDecimal productPrice, Instant at) {
        try {
            DiscountPolicy.validateReplacement(plan, productPrice, at);
        } catch (DiscountOverlapException overlap) {
            log.warn("Product discounts rejected storeId={} productId={} reason=overlap conflictId={}",
                    storeId, productId, overlap.getConflicting().publicId());
            throw overlap;
        }
    }

    /**
     * A row the database refused after the schedule already validated cleanly means another writer
     * claimed an overlapping period in between; the constraint name is logged rather than matched,
     * since an exclusion violation does not report it as reliably as a unique one.
     */
    private DiscountOverlapException concurrentChange(DataIntegrityViolationException violation,
                                                      Long storeId, Long productId) {
        log.warn("Discount overlap detected by constraint storeId={} productId={} cause={}",
                storeId, productId, violation.getMostSpecificCause().getMessage());
        return new DiscountOverlapException("discounts", CONCURRENT_CHANGE, null, null);
    }

    private ProductDiscountEntity toEntity(Discount discount, Long productId, Instant at) {
        ProductDiscountEntity entity = discountEntityMapper.toEntity(discount);
        entity.setProductId(productId);
        entity.setPublicId(discount.publicId() != null
                ? discount.publicId()
                : PublicIdGenerator.generate(PublicIdGenerator.DISCOUNT_PREFIX));
        entity.setCreatedAt(discount.createdAt() != null ? discount.createdAt() : at);
        entity.setUpdatedAt(discount.updatedAt());
        return entity;
    }

    private ProductDiscountView toProductDiscount(Discount discount, BigDecimal price, Instant at) {
        DiscountPricing pricing = DiscountPricing.of(price, discount);
        return new ProductDiscountView(discountEntityMapper.toView(discount, discount.statusAt(at)),
                pricing.finalPrice(), pricing.savings());
    }

    private List<DiscountView> toViews(List<Discount> discounts, Instant at) {
        return discounts.stream()
                .map(discount -> discountEntityMapper.toView(discount, discount.statusAt(at)))
                .toList();
    }
}
