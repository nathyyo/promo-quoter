package com.promoquoter.pricing;

import com.promoquoter.promotion.BuyXGetYPromotion;
import com.promoquoter.promotion.CustomerSegment;
import com.promoquoter.promotion.DiscountTier;
import com.promoquoter.promotion.PercentOffCategoryPromotion;
import com.promoquoter.promotion.Promotion;
import com.promoquoter.promotion.TieredBulkDiscountPromotion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static com.promoquoter.pricing.PricingFixtures.defaultRuleFactory;
import static com.promoquoter.pricing.PricingFixtures.item;
import static com.promoquoter.pricing.PricingFixtures.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class PricingEngineTest {

    private final PricingEngine engine = new PricingEngine(defaultRuleFactory());

    private final PricingItem laptop = item("Laptop", "ELECTRONICS", "1000.00", 3);
    private final PricingItem mouse = item("Mouse", "ELECTRONICS", "25.00", 12);
    private final PricingItem novel = item("Novel", "BOOKS", "15.99", 1);

    @Test
    void withoutPromotionsTheTotalIsTheSubtotal() {
        PricingResult result = engine.price(List.of(laptop, novel), CustomerSegment.REGULAR, List.of());

        assertThat(result.subtotal()).isEqualTo(money("3015.99"));
        assertThat(result.discountTotal()).isEqualTo(money("0.00"));
        assertThat(result.total()).isEqualTo(money("3015.99"));
        assertThat(result.appliedPromotions()).isEmpty();
    }

    @Test
    void appliesCategoryThenVolumeThenFreeItemsWhateverTheCreationOrder() {
        // Created in "wrong" order on purpose: the pipeline must still apply them by stage.
        Promotion buyTwoGetOne = new BuyXGetYPromotion("Laptop B2G1", Set.of(), laptop.productId(), 2, 1);
        Promotion mouseBulk = new TieredBulkDiscountPromotion("Mouse bulk", Set.of(), mouse.productId(), List.of(
                new DiscountTier(10, BigDecimal.valueOf(5)), new DiscountTier(20, BigDecimal.TEN)));
        Promotion electronics = new PercentOffCategoryPromotion("Electronics 10%", Set.of(), "electronics", BigDecimal.TEN);

        PricingResult result = engine.price(List.of(laptop, mouse, novel), CustomerSegment.REGULAR,
                List.of(buyTwoGetOne, mouseBulk, electronics));

        // Laptop: 3000.00 -10% (300.00) = 2700.00, then 1 of 3 free (900.00) = 1800.00
        // Mouse:   300.00 -10% (30.00)  =  270.00, then 5% bulk (13.50)      =  256.50
        // Novel:    15.99, no promotion
        assertThat(result.lines()).extracting(PricedLine::total)
                .containsExactly(money("1800.00"), money("256.50"), money("15.99"));
        assertThat(result.subtotal()).isEqualTo(money("3315.99"));
        assertThat(result.discountTotal()).isEqualTo(money("1243.50"));
        assertThat(result.total()).isEqualTo(money("2072.49"));

        assertThat(result.appliedPromotions())
                .extracting(AppliedPromotion::name, AppliedPromotion::discount)
                .containsExactly(
                        tuple("Electronics 10%", money("330.00")),
                        tuple("Mouse bulk", money("13.50")),
                        tuple("Laptop B2G1", money("900.00")));
    }

    @Test
    void itemisesDiscountsPerLine() {
        Promotion electronics = new PercentOffCategoryPromotion("Electronics 10%", Set.of(), "electronics", BigDecimal.TEN);

        PricedLine line = engine.price(List.of(laptop), CustomerSegment.REGULAR, List.of(electronics)).lines().get(0);

        assertThat(line.productId()).isEqualTo(laptop.productId());
        assertThat(line.unitPrice()).isEqualTo(money("1000.00"));
        assertThat(line.quantity()).isEqualTo(3);
        assertThat(line.subtotal()).isEqualTo(money("3000.00"));
        assertThat(line.discounts()).singleElement().satisfies(discount -> {
            assertThat(discount.promotionId()).isEqualTo(electronics.getId());
            assertThat(discount.amount()).isEqualTo(money("300.00"));
        });
        assertThat(line.discountTotal()).isEqualTo(money("300.00"));
        assertThat(line.total()).isEqualTo(money("2700.00"));
    }

    @Test
    void appliesSegmentRestrictedPromotionsOnlyToThatSegment() {
        Promotion vipBooks = new PercentOffCategoryPromotion("VIP books", Set.of(CustomerSegment.VIP), "books",
                BigDecimal.valueOf(50));

        PricingResult regular = engine.price(List.of(novel), CustomerSegment.REGULAR, List.of(vipBooks));
        PricingResult vip = engine.price(List.of(novel), CustomerSegment.VIP, List.of(vipBooks));

        assertThat(regular.total()).isEqualTo(money("15.99"));
        assertThat(regular.auditTrail()).isEmpty();
        // 50% of 15.99 is a 7.995 discount, rounded HALF_UP to 8.00, leaving 7.99 to pay.
        assertThat(vip.total()).isEqualTo(money("7.99"));
        assertThat(vip.customerSegment()).isEqualTo(CustomerSegment.VIP);
    }

    @Test
    void stackedPromotionsNeverMakeThePriceNegative() {
        Promotion free = new PercentOffCategoryPromotion("All free", Set.of(), "electronics", BigDecimal.valueOf(100));
        Promotion buyOneGetOne = new BuyXGetYPromotion("B1G1", Set.of(), laptop.productId(), 1, 1);

        PricingResult result = engine.price(List.of(laptop), CustomerSegment.REGULAR, List.of(free, buyOneGetOne));

        assertThat(result.total()).isEqualTo(money("0.00"));
        assertThat(result.discountTotal()).isEqualTo(result.subtotal());
        assertThat(result.appliedPromotions()).extracting(AppliedPromotion::name).containsExactly("All free");
    }

    @Test
    void auditTrailRecordsEveryEvaluatedPromotionInOrder() {
        Promotion unrelated = new BuyXGetYPromotion("Other product", Set.of(), mouse.productId(), 1, 1);
        Promotion books = new PercentOffCategoryPromotion("Books 10%", Set.of(), "books", BigDecimal.TEN);

        PricingResult result = engine.price(List.of(novel), CustomerSegment.REGULAR, List.of(unrelated, books));

        assertThat(result.auditTrail())
                .extracting(AuditEntry::step, AuditEntry::promotionName, AuditEntry::totalBefore, AuditEntry::totalAfter)
                .containsExactly(
                        tuple(1, "Books 10%", money("15.99"), money("14.39")),
                        tuple(2, "Other product", money("14.39"), money("14.39")));
        assertThat(result.appliedPromotions()).extracting(AppliedPromotion::name).containsExactly("Books 10%");
    }

    @Test
    void totalsAlwaysEqualTheSumOfTheLines() {
        Promotion electronics = new PercentOffCategoryPromotion("Odd percent", Set.of(), "electronics",
                new BigDecimal("33.33"));

        PricingResult result = engine.price(List.of(laptop, mouse), CustomerSegment.REGULAR, List.of(electronics));

        BigDecimal sumOfLines = result.lines().stream().map(PricedLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumOfApplied = result.appliedPromotions().stream()
                .map(AppliedPromotion::discount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(result.total()).isEqualByComparingTo(sumOfLines);
        assertThat(result.discountTotal()).isEqualByComparingTo(sumOfApplied);
        assertThat(result.total().scale()).isEqualTo(2);
    }
}
