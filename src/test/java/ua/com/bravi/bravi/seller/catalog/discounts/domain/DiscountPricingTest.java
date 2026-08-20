package ua.com.bravi.bravi.seller.catalog.discounts.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DiscountPricingTest {

    private static Discount discount(DiscountType type, String value) {
        return new Discount(1L, "dsc_1", 1L, type, new BigDecimal(value),
                Instant.parse("2026-01-01T00:00:00Z"), null, null, null, null);
    }

    @Test
    void percentDiscountOfTheConceptExample() {
        DiscountPricing pricing = DiscountPricing.of(new BigDecimal("1200"), discount(DiscountType.PERCENT, "20"));

        assertThat(pricing.finalPrice()).isEqualByComparingTo("960.00");
        assertThat(pricing.savings()).isEqualByComparingTo("240.00");
    }

    @Test
    void amountDiscountSubtractsTheSum() {
        DiscountPricing pricing = DiscountPricing.of(new BigDecimal("199.99"), discount(DiscountType.AMOUNT, "50"));

        assertThat(pricing.finalPrice()).isEqualByComparingTo("149.99");
        assertThat(pricing.savings()).isEqualByComparingTo("50.00");
    }

    @Test
    void bothOutputsCarryExactlyTwoDecimals() {
        DiscountPricing pricing = DiscountPricing.of(new BigDecimal("10.0000"), discount(DiscountType.PERCENT, "33"));

        assertThat(pricing.finalPrice().scale()).isEqualTo(2);
        assertThat(pricing.savings().scale()).isEqualTo(2);
        assertThat(pricing.finalPrice()).isEqualByComparingTo("6.70");
        assertThat(pricing.savings()).isEqualByComparingTo("3.30");
    }

    @Test
    void savingsRoundHalfUp() {
        // 5% of 10.10 is 0.505, which rounds up to 0.51 rather than to the nearest even.
        DiscountPricing pricing = DiscountPricing.of(new BigDecimal("10.10"), discount(DiscountType.PERCENT, "5"));

        assertThat(pricing.savings()).isEqualByComparingTo("0.51");
        assertThat(pricing.finalPrice()).isEqualByComparingTo("9.59");
    }

    @Test
    void finalPriceAndSavingsAlwaysAddBackUpToTheOriginal() {
        for (String price : new String[]{"1200", "199.99", "10.10", "0.03", "7777.77"}) {
            DiscountPricing pricing = DiscountPricing.of(new BigDecimal(price), discount(DiscountType.PERCENT, "33"));

            assertThat(pricing.finalPrice().add(pricing.savings()))
                    .as("price %s", price)
                    .isEqualByComparingTo(pricing.originalPrice());
        }
    }

    @Test
    void aStaleAmountDiscountNeverPricesBelowZero() {
        DiscountPricing pricing = DiscountPricing.of(new BigDecimal("40"), discount(DiscountType.AMOUNT, "150"));

        assertThat(pricing.finalPrice()).isEqualByComparingTo("0.00");
    }
}
